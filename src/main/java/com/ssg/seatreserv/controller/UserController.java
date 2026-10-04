package com.ssg.seatreserv.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssg.seatreserv.api.request.CreateUserRequest;
import com.ssg.seatreserv.api.response.CreateUserResponse;
import com.ssg.seatreserv.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	UserService userService;
	
	@PostMapping("/createUser")
	public CreateUserResponse createUser(@RequestBody CreateUserRequest request) {
		return userService.createUser(request);
	}
	
}
