package com.ssg.seatreserv.api.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;


public record CreateShowRequest(
        @NotBlank
        String name,

        @NotEmpty
        List<@NotBlank String> seats,

        @PositiveOrZero
        long pricePaise,

        @Positive
        Integer perUserLimit
) {
}