package com.ssg.seatreserv.api.response;

import java.util.List;
import java.util.UUID;

public record ReserveSeatResponse(UUID reserveId, UUID userId, List<SeatResponse> seats, String idempotencyKey,
		UUID showId, String result, long totalAmount) {

}
