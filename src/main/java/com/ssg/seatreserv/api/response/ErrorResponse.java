package com.ssg.seatreserv.api.response;

import java.time.Instant;

import com.ssg.seatreserv.api.helper.ErrorCodeMaster;

public record ErrorResponse(
		ErrorCodeMaster code,
        String message,
        String requestId,
        Instant timestamp
) {
}
