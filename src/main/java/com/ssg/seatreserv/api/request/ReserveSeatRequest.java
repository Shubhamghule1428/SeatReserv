package com.ssg.seatreserv.api.request;

import java.util.List;
import java.util.UUID;

public record ReserveSeatRequest(List<String> seats, String idempotencyKey, UUID showId) {
	
}
