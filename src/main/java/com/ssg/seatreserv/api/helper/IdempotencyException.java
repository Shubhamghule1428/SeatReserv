package com.ssg.seatreserv.api.helper;

public class IdempotencyException extends RuntimeException {

    public IdempotencyException(String message) {
        super(message);
    }
}
