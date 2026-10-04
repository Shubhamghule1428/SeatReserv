package com.ssg.seatreserv.api.helper;

public class ShowNotFoundException extends RuntimeException {

    public ShowNotFoundException(String showId) {
        super("Show not found: " + showId);
    }
}
