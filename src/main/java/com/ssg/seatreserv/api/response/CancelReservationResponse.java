package com.ssg.seatreserv.api.response;

import java.util.List;
import java.util.UUID;

import com.ssg.seatreserv.api.helper.ReservationStatus;

public record CancelReservationResponse(
        UUID reservationId,
        String showId,
        String userId,
        List<String> seats,
        ReservationStatus status
) {
}