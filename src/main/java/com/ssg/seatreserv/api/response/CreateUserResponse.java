package com.ssg.seatreserv.api.response;

import java.util.UUID;

public record CreateUserResponse(UUID userId, String name, String token, long mobNo) {

}
