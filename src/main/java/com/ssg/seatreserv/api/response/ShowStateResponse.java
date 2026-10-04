package com.ssg.seatreserv.api.response;

import java.util.List;

public record ShowStateResponse(
        String showId,
        String name,
        long pricePaise,
        int perUserLimit,
        long totalSeats,
        Long availableSeats,
        Long heldSeats,
        Long confirmedSeats,
        List<SeatResponse> seats
) {
}