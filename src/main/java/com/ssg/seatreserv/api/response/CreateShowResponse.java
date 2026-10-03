package com.ssg.seatreserv.api.response;

import java.util.List;
import java.util.UUID;

public record CreateShowResponse(String name,
		List<SeatResponse> seats,
		long pricePaisem,
		int userLimt,
		UUID showId,
		String result) {
	
	
}
