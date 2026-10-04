package com.ssg.seatreserv.api.helper;

public class BookingLimitExceededException extends RuntimeException {

    public BookingLimitExceededException(String message) {
        super(message);
    }
}