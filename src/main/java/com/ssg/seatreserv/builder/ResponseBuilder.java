package com.ssg.seatreserv.builder;

import java.util.List;
import java.util.UUID;

import com.ssg.seatreserv.api.request.CreateShowRequest;
import com.ssg.seatreserv.api.request.CreateUserRequest;
import com.ssg.seatreserv.api.response.CreateShowResponse;
import com.ssg.seatreserv.api.response.CreateUserResponse;
import com.ssg.seatreserv.api.response.SeatResponse;

public class ResponseBuilder {

	public static CreateShowResponse createResponse(CreateShowRequest request, UUID showId, String result, List<SeatResponse> seatStatus, int userLimit) {
		CreateShowResponse response = new CreateShowResponse(request.name(), seatStatus, request.pricePaise(), userLimit, showId, result);
		return response;
	}
	
	public static CreateUserResponse createUserResponse(CreateUserRequest request, UUID userId, String token) {
		CreateUserResponse resp = new CreateUserResponse(userId, request.name(), token, request.mobNo());
		
		return resp;
	}
	
	
}
