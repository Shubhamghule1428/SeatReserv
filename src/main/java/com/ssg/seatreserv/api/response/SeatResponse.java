package com.ssg.seatreserv.api.response;

import com.ssg.seatreserv.api.helper.SeatStatus;

public record SeatResponse(String seatname, SeatStatus status) {
	
}
