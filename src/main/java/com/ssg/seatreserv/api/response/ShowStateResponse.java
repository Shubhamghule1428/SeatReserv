package com.ssg.seatreserv.api.response;

import java.util.List;
import java.util.UUID;

public record ShowStateResponse(
        UUID showId,
        String name,
        long pricePaise,
        int perUserLimit,
        int totalSeats,
        int availableSeats,
        int heldSeats,
        int confirmedSeats,
        List<SeatResponse> seats
) {
}