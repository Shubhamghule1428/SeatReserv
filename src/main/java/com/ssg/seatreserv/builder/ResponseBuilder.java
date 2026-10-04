package com.ssg.seatreserv.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.ssg.seatreserv.api.helper.ReservationStatus;
import com.ssg.seatreserv.api.helper.SeatStatus;
import com.ssg.seatreserv.api.request.CancelReservationRequest;
import com.ssg.seatreserv.api.request.CreateShowRequest;
import com.ssg.seatreserv.api.request.CreateUserRequest;
import com.ssg.seatreserv.api.response.CancelReservationResponse;
import com.ssg.seatreserv.api.response.CreateShowResponse;
import com.ssg.seatreserv.api.response.CreateUserResponse;
import com.ssg.seatreserv.api.response.ReserveSeatResponse;
import com.ssg.seatreserv.api.response.SeatResponse;
import com.ssg.seatreserv.api.response.ShowStateResponse;
import com.ssg.seatreserv.entity.Seat;
import com.ssg.seatreserv.entity.Show;

public class ResponseBuilder {

	public static CreateShowResponse createShowResponse(CreateShowRequest request, String showId, String result,
			List<String> seatNames, int userLimit) {
		List<SeatResponse> seatStatus = createSeatResponse(seatNames,SeatStatus.AVAILABLE);
		CreateShowResponse response = new CreateShowResponse(request.name(), seatStatus, request.pricePaise(),
				userLimit, showId, result);
		return response;
	}

	public static CreateUserResponse createUserResponse(CreateUserRequest request, UUID userId, String token) {
		CreateUserResponse resp = new CreateUserResponse(userId, request.name(), token, request.mobNo());

		return resp;
	}

	public static ShowStateResponse buildShowStateResponse(Show show, List<Seat> seats) {

//		Map<String, Long> statusCount = seats.stream().filter(seat -> seat != null)
//				.collect(Collectors.groupingBy(seat -> seat.getStatus().name(), Collectors.counting()));
//
//		List<SeatResponse> seatResps = seats.stream().filter(seat -> seat != null).map(seat -> {
//			SeatResponse seatResp = new SeatResponse(seat.getSeatNo(), seat.getStatus());
//			return seatResp;
//		}).toList();

		Map<String, Long> statusCount = new HashMap<>();
		List<SeatResponse> seatResps = new ArrayList<>();

		for (Seat seat : seats) {

			if (seat == null) {
				continue;
			}

			statusCount.merge(seat.getStatus().name(), 1L, Long::sum);

			seatResps.add(new SeatResponse(seat.getSeatNumber(), seat.getStatus()));
		}
//		seatResps = seatResps.stream().sorted(Comparator.comparing(SeatResponse::seatname)).collect(Collectors.toList());

		return new ShowStateResponse(show.getShowId(), show.getName(), show.getPricePaise(), show.getUserSeatLimit(),
				show.getTotalSeat(), statusCount.getOrDefault(SeatStatus.AVAILABLE.name(), 0L),
				statusCount.getOrDefault(SeatStatus.HELD.name(), 0L),
				statusCount.getOrDefault(SeatStatus.CONFIRMED.name(), 0L), seatResps);
	}

	public static ReserveSeatResponse buildReserveSeatResponse(List<UUID> reserveIds, String UserId, List<String> seats,
			String idemKey, String showId, long totalAmount) {

		List<SeatResponse> seatStatus = createSeatResponse(seats,SeatStatus.CONFIRMED);

		return new ReserveSeatResponse(reserveIds, UserId, seatStatus, idemKey, showId, "Booking done", totalAmount);
	}

	private static List<SeatResponse> createSeatResponse(List<String> seatNames, SeatStatus status) {

		return seatNames.stream().filter(Objects::nonNull).map(String::trim)
				.filter(name -> !name.isBlank()).map(name -> {
					SeatResponse resp = new SeatResponse(name, status);
					return resp;
				}).toList();
	}
	
	
	public static CancelReservationResponse buildCancelReservationResponse(CancelReservationRequest request, String seatName, String userId) {
		return new CancelReservationResponse(request.reservationId(),request.showId(),userId,List.of(seatName),ReservationStatus.CANCELLED);
	}
}
