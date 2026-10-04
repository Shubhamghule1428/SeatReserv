# Stage 1: Build the Spring Boot JAR
FROM gradle:9.7.0-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon -x test

# Stage 2: Unified Container with MySQL 8.0 & Java 21 JRE
FROM mysql:8.0

# Copy Java 21 JRE from Temurin
COPY --from=eclipse-temurin:21-jre /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Copy custom InnoDB configuration
COPY my.cnf /etc/mysql/conf.d/custom-innodb.cnf
RUN chmod 644 /etc/mysql/conf.d/custom-innodb.cnf

# Prepare application working directory
WORKDIR /app

# Copy JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy entrypoint script, strip Windows CRLF line breaks, and make executable
COPY entrypoint.sh /app/entrypoint.sh
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

# Expose Spring Boot web port and internal MySQL port
EXPOSE 10000 3306

ENTRYPOINT ["/app/entrypoint.sh"]