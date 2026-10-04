package com.ssg.seatreserv.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ssg.seatreserv.api.request.CancelReservationRequest;
import com.ssg.seatreserv.api.request.CreateShowRequest;
import com.ssg.seatreserv.api.request.ReserveSeatRequest;
import com.ssg.seatreserv.api.response.CancelReservationResponse;
import com.ssg.seatreserv.api.response.CreateShowResponse;
import com.ssg.seatreserv.api.response.ReserveSeatResponse;
import com.ssg.seatreserv.api.response.ShowStateResponse;
import com.ssg.seatreserv.service.ShowService;

@RestController
public class ShowController {

    @Autowired
    ShowService showService;

    @PostMapping("/createShow")
    public CreateShowResponse createShow(
            @RequestBody CreateShowRequest requestBody) {

        return showService.createShow(requestBody);
    }

    @GetMapping("/show/{id}")
    public ShowStateResponse getShow(@PathVariable("id") String id) {

        return showService.showState(id);
    }
    
    @PostMapping("/reserveSeats")
    public ReserveSeatResponse reserveSeat(@RequestBody ReserveSeatRequest request) {
    	return showService.reserveSeat(request);
    }
    
    @PostMapping("/cancelSeat")
    public CancelReservationResponse cancelReservation(@RequestBody CancelReservationRequest request) {
    	return showService.cancelReservation(request);
    }
}