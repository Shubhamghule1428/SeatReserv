package com.ssg.seatreserv.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssg.seatreserv.api.request.CreateShowRequest;
import com.ssg.seatreserv.api.response.CreateShowResponse;
import com.ssg.seatreserv.service.ShowService;

@RestController
@RequestMapping("/show")
public class ShowController {

	@Autowired
	ShowService showService;
	
	
	public CreateShowResponse createShow(@RequestBody CreateShowRequest requestBody) {

		showService.createShow(requestBody);
		
		return null;
	}
}
