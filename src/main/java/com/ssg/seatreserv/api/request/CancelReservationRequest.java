package com.ssg.seatreserv.api.request;

import java.util.UUID;

public record CancelReservationRequest(String showId, UUID reservationId) {

}
