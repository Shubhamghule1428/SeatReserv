package com.ssg.seatreserv.api.response;

import java.util.List;
import java.util.UUID;

public record ReserveSeatResponse(List<UUID> reserveId, String userId, List<SeatResponse> seats, String idempotencyKey,
		String showId, String result, long totalAmount) {

}
