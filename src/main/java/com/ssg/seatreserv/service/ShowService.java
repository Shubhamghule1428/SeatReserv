package com.ssg.seatreserv.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ssg.seatreserv.api.helper.BookingLimitExceededException;
import com.ssg.seatreserv.api.helper.IdempotencyException;
import com.ssg.seatreserv.api.helper.InvalidReservationRequestException;
import com.ssg.seatreserv.api.helper.ReservationStatus;
import com.ssg.seatreserv.api.helper.SeatStatus;
import com.ssg.seatreserv.api.helper.SeatUnavailableException;
import com.ssg.seatreserv.api.helper.ShowNotFoundException;
import com.ssg.seatreserv.api.request.CancelReservationRequest;
import com.ssg.seatreserv.api.request.CreateShowRequest;
import com.ssg.seatreserv.api.request.ReserveSeatRequest;
import com.ssg.seatreserv.api.response.CancelReservationResponse;
import com.ssg.seatreserv.api.response.CreateShowResponse;
import com.ssg.seatreserv.api.response.ReserveSeatResponse;
import com.ssg.seatreserv.api.response.ShowStateResponse;
import com.ssg.seatreserv.builder.ResponseBuilder;
import com.ssg.seatreserv.entity.IdempotencyRecord;
import com.ssg.seatreserv.entity.Reservation;
import com.ssg.seatreserv.entity.Seat;
import com.ssg.seatreserv.entity.Show;
import com.ssg.seatreserv.repository.IdempotencyRepository;
import com.ssg.seatreserv.repository.ReservationRepository;
import com.ssg.seatreserv.repository.SeatRepository;
import com.ssg.seatreserv.repository.ShowRepository;
import com.ssg.seatreserv.repository.UserRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ShowService {

	private static final Logger log = LoggerFactory.getLogger(ShowService.class);

	ShowRepository showRepo;

	SeatRepository seatRepo;

	IdempotencyRepository idempotencyRepo;

	ReservationRepository reservationRepo;

	UserRepository userRepo;

	ObjectMapper objectMapper;

	private final Counter successCounter;
	private final Counter conflictCounter;
	private final Counter limitExceededCounter;
	private final Counter idempotencyHits;
	private final Timer bookingTimer;

	public ShowService(ShowRepository showRepo, SeatRepository seatRepo, IdempotencyRepository idempotencyRepo,
			ReservationRepository reservationRepo, UserRepository userRepo, ObjectMapper objectMapper,
			MeterRegistry registry) {
		this.showRepo = showRepo;
		this.seatRepo = seatRepo;
		this.idempotencyRepo = idempotencyRepo;
		this.reservationRepo = reservationRepo;
		this.userRepo = userRepo;
		this.objectMapper = objectMapper;

		this.successCounter = registry.counter("seat_booking_attempts_total", "status", "success");
		this.conflictCounter = registry.counter("seat_booking_attempts_total", "status", "conflict");
		this.limitExceededCounter = registry.counter("seat_booking_attempts_total", "status", "limit_exceeded");
		this.idempotencyHits = registry.counter("idempotency_cache_hits_total");
		this.bookingTimer = registry.timer("seat_booking_latency_seconds");
	}

	@Value("${app.user.maxseatlimit:4}")
	int maxUserSeatLimit;

	@Transactional
	public CreateShowResponse createShow(CreateShowRequest requestBody) {
		List<String> seatNames = requestBody.seats();
		int userLimit = requestBody.perUserLimit() != 0 ? requestBody.perUserLimit() : maxUserSeatLimit;
		Show show = new Show(requestBody.name(), requestBody.pricePaise(), seatNames.size(),
				requestBody.perUserLimit());

		show = showRepo.save(show);

		final String showId = show.getShowId();

		List<Seat> seats = seatNames.stream().map(seatName -> Seat.createSeat(showId, seatName)).toList();

		seatRepo.saveAll(seats);

		return ResponseBuilder.createShowResponse(requestBody, showId, "Show Created SucessFully!!", seatNames,
				userLimit);
	}

	// check show status
	public ShowStateResponse showState(String showId) {
		Optional<Show> show = showRepo.findById(showId);
		if (show.isPresent()) {
			List<Seat> seats = seatRepo.findByShowId(showId);
			return ResponseBuilder.buildShowStateResponse(show.get(), seats);
		}
		log.info("NO show present with showid" + showId);
		return null;
	}

	@Transactional
	public ReserveSeatResponse reserveSeat(ReserveSeatRequest request) {

		Timer.Sample sample = Timer.start();

		try {
			UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

			if (request.seats() == null || request.seats().isEmpty()) {
				throw new InvalidReservationRequestException("At least one seat must be selected");
			}
			
			// Prevent duplicate seats like ["A1", "A1", "A2"]
			if (request.seats().stream().distinct().count() != request.seats().size()) {

				throw new InvalidReservationRequestException("Duplicate seats are not allowed");
			}

			Show show = showRepo.findById(request.showId())
					.orElseThrow(() -> new ShowNotFoundException(request.showId()));

			String idempotencyKey = request.idempotencyKey();
			String requestStr = objectMapper.writeValueAsString(request);
			String requestHash = sha256(requestStr);
			
			Optional<IdempotencyRecord> existing = idempotencyRepo.findById(idempotencyKey);

			if (existing.isPresent()) {

				idempotencyHits.increment();

				try {
					if(requestHash != null && requestHash.equals(existing.get().getRequestHash())) {
						throw new IdempotencyException("Request tampering: not matched request hash");
					}
					return objectMapper.readValue(existing.get().getResponseBody(), ReserveSeatResponse.class);

				} catch (Exception e) {
					throw new IdempotencyException("Unable to process stored idempotency response");
				}
			}

			//  Check user booking limit
			long currentBookings = reservationRepo.countActiveBookings(userId.toString(), request.showId());

			long requestedSeatCount = request.seats().size();

			if (currentBookings + requestedSeatCount > show.getUserSeatLimit()) {

				limitExceededCounter.increment();

				throw new BookingLimitExceededException("User booking limit reached");
			}

			int rowsUpdated = seatRepo.reserveSeatsAtomically(request.showId(), request.seats());

			if (rowsUpdated != requestedSeatCount) {

				conflictCounter.increment();

				throw new SeatUnavailableException("One or more seats are already booked or unavailable");
			}

			//Fetch reserved seats
			List<Seat> seats = seatRepo.findByShowIdAndSeatNumbers(request.showId(), request.seats());

			if (seats.size() != requestedSeatCount) {
				throw new SeatUnavailableException("One or more requested seats were not found");
			}

			//  Create reservations
			List<Reservation> reservations = seats.stream().map(seat -> new Reservation(request.showId(),
					seat.getSeatId(), userId.toString(), ReservationStatus.CONFIRMED)).toList();

			List<Reservation> savedReservations = reservationRepo.saveAll(reservations);

			// Collect reservation IDs
			List<UUID> reservationIds = savedReservations.stream().map(Reservation::getId).toList();

			//Calculate total amount
			long totalAmount = show.getPricePaise() * savedReservations.size();

			ReserveSeatResponse response = ResponseBuilder.buildReserveSeatResponse(reservationIds, userId.toString(),
					request.seats(), idempotencyKey, request.showId(), totalAmount);

			try {

				String payload = objectMapper.writeValueAsString(response);
				

				idempotencyRepo.save(new IdempotencyRecord(idempotencyKey, userId.toString(), 200, payload, requestHash));

			} catch (Exception e) {

				throw new IdempotencyException("Error persisting idempotency response");
			}

			log.info("Reservation successful: showId={}, seats={}, userId={}", request.showId(), request.seats(),
					userId);

			return response;

		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			throw new IdempotencyException("Unable to parsing request for idempotency check");
		} finally {
			sample.stop(bookingTimer);
		}
	}

	// cancel reservation
	@Transactional
	public CancelReservationResponse cancelReservation(CancelReservationRequest request) {
		Reservation reservation = reservationRepo
				.findByIdAndShowIdAndStatus(request.reservationId(), request.showId(), ReservationStatus.CONFIRMED)
				.orElseThrow(() -> new InvalidReservationRequestException("Reservation not found"));

		reservation.setStatus(ReservationStatus.CANCELLED);
		reservationRepo.save(reservation);

		Seat seat = seatRepo.findById(reservation.getSeatId())
				.orElseThrow(() -> new SeatUnavailableException("Seat not found"));
		seat.setStatus(SeatStatus.AVAILABLE);
		seatRepo.save(seat);

		return ResponseBuilder.buildCancelReservationResponse(request, seat.getSeatNumber(), reservation.getUserId());
	}

	public static String sha256(String text) throws NoSuchAlgorithmException {

		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		byte[] encodedHash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

		return HexFormat.of().formatHex(encodedHash);
	}
	// check seat status
}
