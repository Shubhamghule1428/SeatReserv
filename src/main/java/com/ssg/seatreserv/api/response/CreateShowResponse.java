package com.ssg.seatreserv.api.response;

import java.util.List;

public record CreateShowResponse(String name,
		List<SeatResponse> seats,
		long pricePaise,
		int userLimt,
		String showId,
		String result) {
	
	
}
