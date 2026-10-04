# Seat Reservation at Scale

A Spring Boot backend for high-concurrency seat reservation with transactional booking, atomic seat allocation, per-user booking limits, idempotency handling, cancellation, token-based authentication, and application observability.

The project is designed around the core problem of **preventing double booking when many users try to reserve the same seats concurrently**.

> **Status:** Take-home / backend engineering exercise  
> **Java:** 21  
> **Spring Boot:** 4.1.1  
> **Build:** Gradle  
> **Database:** MySQL 8  
> **ORM:** Spring Data JPA / Hibernate

---

## 1. Problem Statement

The service exposes APIs to:

- Create users
- Create shows with a configurable list of seats
- View the current state of a show and its seats
- Reserve one or more seats
- Cancel reservations
- Prevent duplicate bookings under concurrent requests
- Enforce a maximum number of seats per user
- Make reservation requests idempotent
- Expose health, metrics, and Prometheus endpoints

The most important part of the implementation is the booking path.

Instead of:

1. Read seat
2. Check `AVAILABLE`
3. Update seat

the reservation flow uses an **atomic database update**:

```sql
UPDATE seats
SET status = CONFIRMED,
    version = version + 1
WHERE show_id = ?
  AND seat_no IN (...)
  AND status = AVAILABLE;
```

The application then verifies that the number of updated rows matches the number of requested seats.

This makes the database the final authority for seat ownership and avoids a classic read-check-write race.

---

## 2. Architecture

```text
                         ┌──────────────────────┐
                         │       Client         │
                         │  Postman / Frontend  │
                         └──────────┬───────────┘
                                    │ HTTP
                                    ▼
                         ┌──────────────────────┐
                         │  Spring Boot API     │
                         │      :8080           │
                         └──────────┬───────────┘
                                    │
                    ┌───────────────┼────────────────┐
                    │               │                │
                    ▼               ▼                ▼
             Security Filter   Controllers       Actuator
                    │               │                │
                    ▼               ▼                ▼
             Token Auth        Services         Metrics
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │ Spring Data JPA      │
                         │ / Hibernate          │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │      MySQL 8         │
                         │                      │
                         │ users                │
                         │ shows                │
                         │ seats                │
                         │ reservations         │
                         │ idempotency_keys     │
                         └──────────────────────┘
```

### Main packages

```text
com.ssg.seatreserv
├── api
│   ├── helper
│   ├── request
│   └── response
├── builder
├── controller
├── entity
├── repository
├── security
├── service
└── SeatreservApplication
```

---

## 3. Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Application runtime |
| Spring Boot 4.1.1 | Backend framework |
| Spring Web | REST APIs |
| Spring Data JPA | Persistence |
| Hibernate | ORM |
| Spring Security | Request authentication |
| Jakarta Validation | DTO validation annotations |
| MySQL 8 | Relational database |
| HikariCP | JDBC connection pooling |
| Micrometer | Application metrics |
| Prometheus registry | Metrics export |
| Gradle | Build and dependency management |
| Docker | Containerization |
| Docker Compose | Local MySQL + application orchestration |

---

# 4. Getting Started

## Prerequisites

Install:

- Java 21
- Git
- Docker Desktop
- Docker Compose
- Gradle is optional because the repository contains the Gradle wrapper

Verify:

```bash
java -version
docker --version
docker compose version
```

---

# 5. Configuration

The application reads configuration from `application.properties`.

Important properties:

```properties
server.port=8080

spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/seatdb}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}

spring.datasource.hikari.maximum-pool-size=${DB_POOL_SIZE:20}
spring.datasource.hikari.minimum-idle=${DB_MIN_IDLE:5}
spring.datasource.hikari.connection-timeout=${DB_CONNECTION_TIMEOUT:3000}

app.user.maxseatlimit=${MAX_USER_SEAT_LIMIT:3}
```

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/seatdb...` | JDBC connection URL |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | — | Database password |
| `DB_POOL_SIZE` | `20` | Hikari maximum pool size |
| `DB_MIN_IDLE` | `5` | Minimum idle connections |
| `DB_CONNECTION_TIMEOUT` | `3000` | Connection acquisition timeout |
| `MAX_USER_SEAT_LIMIT` | `3` | Application fallback seat limit |

### Security note

Do **not** commit real database passwords or secrets.

The provided project archive contains environment/configuration files with a database password. Those values should be rotated and removed from source control before publishing the repository.

Recommended approach:

```bash
export DB_PASSWORD='<strong-password>'
```

or use a local `.env` file that is excluded by `.gitignore`.

---

# 6. Database Setup

The application expects a MySQL database named:

```text
seatdb
```

Create it:

```sql
CREATE DATABASE seatdb;
```

The application currently uses:

```properties
spring.jpa.hibernate.ddl-auto=none
```

Therefore, **Hibernate will not automatically create or update the tables**.

Before starting the application against a fresh database, the following tables need to exist:

```text
users
shows
seats
reservations
idempotency_keys
```

A production-quality version should add a database migration tool such as **Flyway** or **Liquibase** and commit the schema migrations to the repository.

---

# 7. Running Locally

## Option A — Run with Gradle

From the project root:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

Application:

```text
http://localhost:8080
```

---

## Option B — Build the JAR

```bash
./gradlew clean build
```

Run:

```bash
java -jar build/libs/seatreserv-0.0.1-SNAPSHOT.jar
```

---

# 8. Docker

## Build the image

From the project root:

```bash
docker build -t seatreserv:latest .
```

Check:

```bash
docker images
```

Run:

```bash
docker run --rm \
  -p 8080:8080 \
  -e DB_URL='jdbc:mysql://host.docker.internal:3306/seatdb' \
  -e DB_USERNAME='root' \
  -e DB_PASSWORD='<password>' \
  seatreserv:latest
```

> The supplied Dockerfile combines MySQL and the Spring Boot application into one image. That is convenient for the exercise, but it is **not the recommended production architecture**. Production deployments should normally run the application and database as separate services.

---

# 9. Docker Compose

The repository contains a Compose configuration under:

```text
src/main/resources/docker-compose.yml
```

For reliable use, the Compose file should be moved to the repository root so its build context resolves the root `Dockerfile` and supporting files correctly.

Recommended root-level structure:

```text
seatreserv/
├── Dockerfile
├── docker-compose.yml
├── entrypoint.sh
├── my.cnf
├── build.gradle
├── gradlew
└── src/
```

Then:

```bash
docker compose up --build
```

Stop:

```bash
docker compose down
```

Remove database volume as well:

```bash
docker compose down -v
```

---

# 10. Authentication

The application uses a simple token-based authentication mechanism.

Create a user first:

```http
POST /user/createUser
Content-Type: application/json
```

Request:

```json
{
  "name": "Rahul",
  "mobNo": 9876543210
}
```

Example response:

```json
{
  "userId": "0c2d1c1a-7b9d-4a6f-9f12-123456789abc",
  "name": "Rahul",
  "token": "generated-sha256-token",
  "mobNo": 9876543210
}
```

For protected APIs, pass the returned token directly in the `Authorization` header:

```http
Authorization: generated-sha256-token
```

> This implementation is intentionally lightweight for the exercise. It is not JWT/OAuth2 authentication.

---

# 11. API Endpoints

Base URL:

```text
http://localhost:8080
```

## API Summary

| Method | Endpoint | Authentication | Purpose |
|---|---|---:|---|
| POST | `/user/createUser` | No | Create a user |
| POST | `/createShow` | No | Create a show |
| GET | `/show/{showId}` | No | Get show and seat state |
| POST | `/reserveSeats` | Yes | Reserve seats |
| POST | `/cancelSeat` | Yes | Cancel a reservation |
| GET | `/actuator/health` | No* | Health check |
| GET | `/actuator/metrics` | No* | Metrics |
| GET | `/actuator/prometheus` | No* | Prometheus metrics |

\* Availability can depend on the active Spring Security configuration.

---

# 12. Create User

### Request

```http
POST /user/createUser
Content-Type: application/json
```

```json
{
  "name": "Rahul",
  "mobNo": 9876543210
}
```

### Response

```json
{
  "userId": "0c2d1c1a-7b9d-4a6f-9f12-123456789abc",
  "name": "Rahul",
  "token": "sha256-token",
  "mobNo": 9876543210
}
```

Save the `token`.

---

# 13. Create Show

### Request

```http
POST /createShow
Content-Type: application/json
```

```json
{
  "name": "Avengers Endgame",
  "seats": [
    "A1",
    "A2",
    "A3",
    "A4",
    "A5",
    "B1",
    "B2",
    "B3"
  ],
  "pricePaise": 25000,
  "perUserLimit": 3
}
```

`pricePaise` uses paise rather than rupees.

For example:

```text
25000 paise = ₹250
```

### Response

```json
{
  "name": "Avengers Endgame",
  "seats": [
    {
      "seatname": "A1",
      "status": "AVAILABLE"
    },
    {
      "seatname": "A2",
      "status": "AVAILABLE"
    }
  ],
  "pricePaise": 25000,
  "userLimt": 3,
  "showId": "Avengers Endgame-AB12CD34",
  "result": "Show Created SucessFully!!"
}
```

---

# 14. Get Show State

### Request

```http
GET /show/{showId}
```

Example:

```http
GET /show/Avengers%20Endgame-AB12CD34
```

### Response

```json
{
  "showId": "Avengers Endgame-AB12CD34",
  "name": "Avengers Endgame",
  "pricePaise": 25000,
  "perUserLimit": 3,
  "totalSeats": 8,
  "availableSeats": 6,
  "heldSeats": 0,
  "confirmedSeats": 2,
  "seats": [
    {
      "seatname": "A1",
      "status": "CONFIRMED"
    },
    {
      "seatname": "A2",
      "status": "AVAILABLE"
    }
  ]
}
```

Possible seat states:

```text
AVAILABLE
HELD
CONFIRMED
```

---

# 15. Reserve Seats

This is the main concurrency-sensitive endpoint.

### Request

```http
POST /reserveSeats
Authorization: <user-token>
Content-Type: application/json
```

```json
{
  "showId": "Avengers Endgame-AB12CD34",
  "seats": [
    "A1",
    "A2"
  ],
  "idempotencyKey": "booking-123456789"
}
```

### Successful response

```json
{
  "reserveId": [
    "6fd7c0c2-1234-4b5e-9d11-123456789abc",
    "c8d1a5b7-5678-4c90-8d21-987654321abc"
  ],
  "userId": "0c2d1c1a-7b9d-4a6f-9f12-123456789abc",
  "seats": [
    {
      "seatname": "A1",
      "status": "CONFIRMED"
    },
    {
      "seatname": "A2",
      "status": "CONFIRMED"
    }
  ],
  "idempotencyKey": "booking-123456789",
  "showId": "Avengers Endgame-AB12CD34",
  "result": "Booking done",
  "totalAmount": 50000
}
```

### Important rules

- Requested seat list must not be empty.
- Duplicate seat names are rejected.
- Requested seats must exist.
- Seats must be available.
- The user cannot exceed the configured per-show seat limit.
- The database performs the atomic seat state transition.
- Reservation records are created after successful seat allocation.
- An idempotency record is stored for the request.

---

# 16. Cancellation

### Request

```http
POST /cancelSeat
Authorization: <user-token>
Content-Type: application/json
```

```json
{
  "showId": "Avengers Endgame-AB12CD34",
  "reservationId": "6fd7c0c2-1234-4b5e-9d11-123456789abc"
}
```

### Response

```json
{
  "reservationId": "6fd7c0c2-1234-4b5e-9d11-123456789abc",
  "showId": "Avengers Endgame-AB12CD34",
  "userId": "0c2d1c1a-7b9d-4a6f-9f12-123456789abc",
  "seats": [
    "A1"
  ],
  "status": "CANCELLED"
}
```

Cancellation:

1. Finds the confirmed reservation.
2. Marks the reservation as `CANCELLED`.
3. Changes the corresponding seat back to `AVAILABLE`.

---

# 17. Error Handling

The application has a global exception handler.

Example:

```json
{
  "code": "SEAT_UNAVAILABLE",
  "message": "One or more seats are already booked or unavailable",
  "requestId": "2db9a1c2-7b55-42d4-a1bb-123456789abc",
  "timestamp": "2026-10-04T12:30:00Z"
}
```

### Main HTTP status mapping

| HTTP Status | Error |
|---:|---|
| `400` | Invalid request |
| `401` | Missing/invalid authentication |
| `403` | Booking limit exceeded |
| `404` | Show not found |
| `409` | Seat unavailable |
| `409` | Idempotency error |
| `500` | Unexpected server error |

---

# 18. Concurrency Strategy

The booking API is designed for concurrent traffic.

## Problem

A naïve implementation can do:

```text
Thread A -> SELECT seat -> AVAILABLE
Thread B -> SELECT seat -> AVAILABLE

Thread A -> UPDATE seat -> CONFIRMED
Thread B -> UPDATE seat -> CONFIRMED
```

Both requests can believe they successfully acquired the same seat.

That is a double-booking race.

## Implementation

The repository uses a conditional bulk update:

```java
@Modifying
@Query("""
    UPDATE Seat s
    SET s.status = 2,
        s.version = s.version + 1
    WHERE s.showId = :showId
      AND s.seatNumber IN (:seatNumbers)
      AND s.status = 0
""")
int reserveSeatsAtomically(...);
```

The service then checks:

```text
rowsUpdated == requestedSeatCount
```

If the counts differ, at least one requested seat could not be reserved.

The transaction is rolled back and the request returns a conflict.

### Why this approach?

It avoids holding Java-side locks and lets the database serialize the conflicting updates.

This is much more appropriate for a horizontally scaled service than:

```java
synchronized
```

or an in-memory lock.

---

# 19. Optimistic Locking

The `Seat` entity contains:

```java
@Version
private long version;
```

This gives the entity an optimistic-locking version field.

The actual reservation path additionally uses an atomic conditional update, which is important because the main requirement is to make the seat transition itself concurrency-safe.

---

# 20. Idempotency

Reservation requests accept:

```json
{
  "idempotencyKey": "booking-123456789"
}
```

The key is stored in:

```text
idempotency_keys
```

The record contains:

- idempotency key
- user ID
- response code
- serialized response body
- request hash
- creation timestamp

The intended flow is:

```text
Request
   │
   ▼
Check idempotency key
   │
   ├── Existing + same request
   │       └── Return stored response
   │
   ├── Existing + different request
   │       └── Reject
   │
   └── New key
           │
           ▼
       Reserve seats
           │
           ▼
       Persist response
           │
           ▼
       Return response
```

### Important implementation note

The current implementation's request-hash comparison appears reversed:

```java
if (requestHash != null && requestHash.equals(existing.get().getRequestHash())) {
    throw new IdempotencyException(...);
}
```

The intended condition should normally reject when the hashes **do not match**, not when they match.

This should be corrected before treating idempotency as production-ready.

---

# 21. Per-User Booking Limit

Each show has a configured:

```text
perUserLimit
```

Before booking, the application counts active reservations:

```text
existing active reservations
+
requested seats
```

If the total exceeds the show limit, the request is rejected.

Example:

```text
Per-user limit = 3

Existing bookings = 2
Requested seats   = 2

2 + 2 = 4 > 3

Result: rejected
```

This prevents a user from bypassing the limit by making several smaller requests.

---

# 22. Observability

Spring Boot Actuator is enabled.

Configured endpoints:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
```

Custom booking metrics include:

```text
seat_booking_attempts_total{status="success"}
seat_booking_attempts_total{status="conflict"}
seat_booking_attempts_total{status="limit_exceeded"}

idempotency_cache_hits_total

seat_booking_latency_seconds
```

These metrics are useful for a load-test / burst-test scenario.

For example:

```text
Successful bookings
Booking conflicts
Booking latency
Idempotency cache hits
```

can be monitored while sending concurrent reservation requests.

---

# 23. Connection Pooling

HikariCP is used as the JDBC connection pool.

Configured defaults:

```properties
maximum-pool-size=20
minimum-idle=5
connection-timeout=3000
idle-timeout=600000
max-lifetime=1800000
```

The important point is that database concurrency is bounded by the connection pool.

Increasing application threads without considering:

- database connections
- MySQL `max_connections`
- CPU
- disk I/O
- lock contention

does not automatically increase throughput.

---

# 24. Performance Design

The implementation focuses on minimizing the expensive part of the booking path.

### Booking path

```text
Authentication
      ↓
Validate request
      ↓
Load show
      ↓
Check idempotency
      ↓
Check user's active booking count
      ↓
Atomic seat update
      ↓
Create reservation records
      ↓
Persist idempotency response
      ↓
Return response
```

### Important design choices

#### 1. Database-level atomicity

Seat availability is enforced in the SQL update.

#### 2. No synchronized application lock

This allows multiple application instances to process traffic without depending on JVM-local state.

#### 3. Hikari connection pooling

Connections are reused rather than opened for every request.

#### 4. Batch seat persistence

Seat creation uses:

```text
saveAll(...)
```

instead of individually saving every seat.

#### 5. Metrics

Booking latency and outcomes are measured using Micrometer.

---

# 25. Data Model

Conceptually:

```text
users
  │
  │ user_id
  ▼
reservations
  │
  ├── show_id ───────► shows
  │
  └── seat_id ───────► seats

idempotency_keys
  │
  └── user_id
```

### `users`

```text
userid
name
mob_no
token
```

### `shows`

```text
id
name
price
total_seats
user_seat_limit
created_at
```

### `seats`

```text
id
show_id
seat_no
status
version
```

### `reservations`

```text
id
show_id
seat_id
user_id
status
created_at
```

### `idempotency_keys`

```text
key
user_id
response_code
response_body
request_hash
created_at
```

---

# 26. Development Practices / Tactics

## Keep business logic in the service layer

Controllers should remain thin:

```text
HTTP request
   ↓
Controller
   ↓
Service
   ↓
Repository
```

The reservation rules belong in `ShowService`, not inside the controller.

---

## Use DTOs at the API boundary

The project uses Java records for request/response DTOs:

```text
CreateUserRequest
CreateShowRequest
ReserveSeatRequest
CancelReservationRequest

CreateUserResponse
CreateShowResponse
ReserveSeatResponse
CancelReservationResponse
ShowStateResponse
```

This prevents JPA entities from becoming the API contract.

---

## Keep database concurrency in the database

Do not attempt to solve seat contention using:

```java
synchronized
ReentrantLock
ConcurrentHashMap
```

as the source of truth.

Those mechanisms only protect one JVM instance.

The database update protects the shared state.

---

## Use transactions around state changes

Reservation and cancellation are transactional operations.

The goal is to prevent partial state such as:

```text
Seat = CONFIRMED
Reservation = not created
```

or:

```text
Reservation = CANCELLED
Seat = still CONFIRMED
```

---

## Measure before tuning

For burst testing, monitor:

```text
CPU
Memory
GC
DB CPU
DB connections
Hikari active connections
request latency
5xx rate
seat conflicts
booking throughput
```

Do not claim that increasing thread count or connection-pool size improves throughput without measurements.

---

# 27. Testing Strategy

The current repository contains a basic Spring application context test.

For a production-quality implementation, the following tests should be added.

### Unit tests

- Create show
- Create user
- Empty seat list
- Duplicate seats
- Show not found
- Booking limit
- Seat unavailable
- Cancellation
- Idempotency
- Request hash mismatch

### Integration tests

- Create show + seats
- Reserve seats
- Cancel reservation
- Verify database state
- Verify idempotency record
- Verify transaction rollback

### Concurrency tests

The most important test:

```text
100 concurrent requests
        ↓
same show
        ↓
same seat
```

Expected result:

```text
1 successful reservation
99 conflicts
```

For multiple seats:

```text
N available seats
M concurrent requests
```

Expected:

```text
At most N seats become CONFIRMED.
```

---

# 28. Example Burst Test

The project contains:

```text
src/main/java/com/ssg/seatreserv/BurstTest.java
```

The recommended load-test workflow is:

```text
1. Create a show with enough seats
2. Create multiple users
3. Generate concurrent reservation requests
4. Target overlapping seat ranges
5. Measure:
   - success
   - conflict
   - latency
   - DB connections
   - CPU
6. Verify no duplicate confirmed seat exists
```

The key correctness invariant is:

```text
For every show + seat:
    confirmed reservations <= 1
```

---

# 29. Recommended API Test Sequence

Use this sequence in Postman/cURL.

### Step 1 — Create user

```http
POST /user/createUser
```

Save:

```text
token
userId
```

### Step 2 — Create show

```http
POST /createShow
```

Save:

```text
showId
```

### Step 3 — Check show

```http
GET /show/{showId}
```

Verify all seats are:

```text
AVAILABLE
```

### Step 4 — Reserve seats

```http
POST /reserveSeats
Authorization: <token>
```

### Step 5 — Check show again

```http
GET /show/{showId}
```

Verify selected seats are:

```text
CONFIRMED
```

### Step 6 — Retry with same idempotency key

Send the same reservation request again.

The intended behavior is to return the previously stored result rather than create another reservation.

### Step 7 — Cancel

```http
POST /cancelSeat
Authorization: <token>
```

### Step 8 — Check show

The cancelled seat should become:

```text
AVAILABLE
```

---

# 30. Production Improvements

The current implementation is suitable as an engineering exercise, but several areas should be improved before production use.

## High priority

### Database migrations

Add Flyway or Liquibase.

### Fix idempotency comparison

Reject only when the same key is reused with a different request payload.

### Ownership validation

Cancellation should verify that the authenticated user owns the reservation.

### API validation

The DTOs contain Jakarta Validation annotations, but controller methods currently do not consistently use `@Valid`.

For example:

```java
@Valid @RequestBody CreateShowRequest request
```

should be used where appropriate.

### Authentication

Replace the SHA-256 token scheme with a proper authentication mechanism such as:

- OAuth2
- JWT
- opaque tokens backed by a secure token store

### Secrets

Remove passwords and secrets from source files and environment files committed to Git.

---

# 31. Further Scalability Improvements

For significantly higher traffic:

### Redis

Useful for:

- distributed idempotency cache
- rate limiting
- short-lived reservation state
- distributed coordination where genuinely required

But Redis should not blindly replace the database as the final seat ownership source.

### Kafka

Useful for asynchronous workloads such as:

```text
Booking confirmed
       ↓
Kafka
 ├── Notification
 ├── Analytics
 ├── Payment workflow
 └── Audit
```

Do not put Kafka in the synchronous seat-allocation critical path unless the consistency model is deliberately designed around it.

### Database indexing

Add and verify indexes around:

```text
seats(show_id, seat_no)
seats(show_id, status)
reservations(user_id, show_id, status)
idempotency_keys(key)
users(token)
```

Actual indexes should be validated using query plans and production-like data volumes.

### Horizontal scaling

The reservation design should remain safe across multiple application instances because the critical seat update is performed by the database rather than a JVM-local lock.

---

# 32. Known Limitations

This section is intentionally explicit.

1. **No database migration scripts are included.** `ddl-auto=none` means a fresh MySQL database requires schema creation.
2. **Validation annotations are present but request validation is not consistently activated with `@Valid`.**
3. **The idempotency request-hash comparison appears inverted and should be corrected.**
4. **Cancellation does not currently enforce reservation ownership against the authenticated user.**
5. **The token authentication mechanism is intentionally basic and is not suitable as a production authentication solution.**
6. **The project has a `HELD` state but does not currently implement a hold/expiry workflow.**
7. **The supplied Compose file is located under `src/main/resources`, while the Docker build context expects root-level Docker files. Moving Compose to the project root is recommended.**
8. **The supplied Dockerfile runs MySQL and the application in one container. Separate containers/services are preferred for production.**
9. **The repository currently has limited automated test coverage for the most important concurrency scenarios.**
10. **The database password must not be committed to Git. Rotate any credential that has already been exposed.**

---

# 33. Suggested Future Architecture

For a production-scale implementation:

```text
                    ┌───────────────┐
                    │ Load Balancer │
                    └───────┬───────┘
                            │
             ┌──────────────┼──────────────┐
             ▼              ▼              ▼
        ┌─────────┐    ┌─────────┐    ┌─────────┐
        │ App #1  │    │ App #2  │    │ App #3  │
        └────┬────┘    └────┬────┘    └────┬────┘
             │              │              │
             └──────────────┼──────────────┘
                            ▼
                    ┌───────────────┐
                    │    MySQL      │
                    │ Primary/Read  │
                    │   Replicas    │
                    └───────────────┘
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
           ┌─────────┐             ┌─────────┐
           │ Redis   │             │ Kafka   │
           └─────────┘             └─────────┘
```

The key invariant remains:

> **A seat can have only one confirmed owner.**

All scaling decisions should preserve that invariant.

---

# 34. Useful Commands

### Build

```bash
./gradlew clean build
```

### Run

```bash
./gradlew bootRun
```

### Run tests

```bash
./gradlew test
```

### Build Docker image

```bash
docker build -t seatreserv:latest .
```

### Run Compose

```bash
docker compose up --build
```

### Stop Compose

```bash
docker compose down
```

### Remove volumes

```bash
docker compose down -v
```

### View application logs

```bash
docker logs -f seatreserv
```

### View MySQL logs

```bash
docker logs -f seatreserv-mysql
```

---

# 35. Health & Metrics

Health:

```http
GET /actuator/health
```

Metrics:

```http
GET /actuator/metrics
```

Prometheus:

```http
GET /actuator/prometheus
```

Example:

```bash
curl http://localhost:8080/actuator/health
```

---

# 36. Repository Checklist

Before submitting the project:

- [ ] Remove secrets/passwords from tracked files
- [ ] Add database migration scripts
- [ ] Add `@Valid` to validated request bodies
- [ ] Fix idempotency hash comparison
- [ ] Validate reservation ownership during cancellation
- [ ] Add concurrency integration tests
- [ ] Add database indexes
- [ ] Add API documentation/OpenAPI
- [ ] Move `docker-compose.yml` to repository root
- [ ] Verify Docker build from a clean checkout
- [ ] Verify a fresh database can initialize from zero
- [ ] Run full test suite
- [ ] Run burst/concurrency test
- [ ] Verify no double booking
- [ ] Verify idempotent retry behavior
- [ ] Verify booking-limit enforcement
- [ ] Verify cancellation consistency

---

## License

This project is an engineering exercise / demonstration project. Add an explicit license here if the repository is intended for public distribution.
