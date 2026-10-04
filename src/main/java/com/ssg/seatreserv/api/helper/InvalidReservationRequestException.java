package com.ssg.seatreserv.api.helper;

public class InvalidReservationRequestException extends RuntimeException {

    public InvalidReservationRequestException(String message) {
        super(message);
    }
}