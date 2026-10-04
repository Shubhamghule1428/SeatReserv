package com.ssg.seatreserv.controller;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssg.seatreserv.api.helper.BookingLimitExceededException;
import com.ssg.seatreserv.api.helper.ErrorCodeMaster;
import com.ssg.seatreserv.api.helper.IdempotencyException;
import com.ssg.seatreserv.api.helper.InvalidReservationRequestException;
import com.ssg.seatreserv.api.helper.SeatUnavailableException;
import com.ssg.seatreserv.api.helper.ShowNotFoundException;
import com.ssg.seatreserv.api.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
    @ExceptionHandler(ShowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShowNotFound(
            ShowNotFoundException ex,
            HttpServletRequest request) {

        return buildResponse(
                ErrorCodeMaster.SHOW_NOT_FOUND,
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleSeatUnavailable(
            SeatUnavailableException ex,
            HttpServletRequest request) {

        return buildResponse(
                ErrorCodeMaster.SEAT_UNAVAILABLE,
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler(BookingLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleBookingLimitExceeded(
            BookingLimitExceededException ex,
            HttpServletRequest request) {

        return buildResponse(
                ErrorCodeMaster.BOOKING_LIMIT_EXCEEDED,
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                request
        );
    }

    @ExceptionHandler(IdempotencyException.class)
    public ResponseEntity<ErrorResponse> handleIdempotency(
            IdempotencyException ex,
            HttpServletRequest request) {

        return buildResponse(
                ErrorCodeMaster.IDEMPOTENCY_ERROR,
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler(InvalidReservationRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            InvalidReservationRequestException ex,
            HttpServletRequest request) {

        return buildResponse(
                ErrorCodeMaster.INVALID_REQUEST,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {

        log.error(
                "Unexpected exception. requestId={}",
                request.getHeader("X-Request-Id"),
                ex
        );

        return buildResponse(
                ErrorCodeMaster.INTERNAL_SERVER_ERROR,
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            ErrorCodeMaster code,
            String message,
            HttpStatus status,
            HttpServletRequest request) {

        String requestId = request.getHeader("X-Request-Id");

        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        ErrorResponse response = new ErrorResponse(
                code,
                message,
                requestId,
                Instant.now()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
