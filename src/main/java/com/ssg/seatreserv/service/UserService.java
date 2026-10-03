package com.ssg.seatreserv.service;

import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ssg.seatreserv.api.request.CreateUserRequest;
import com.ssg.seatreserv.api.response.CreateUserResponse;
import com.ssg.seatreserv.builder.ResponseBuilder;
import com.ssg.seatreserv.entity.User;
import com.ssg.seatreserv.repository.UserRepository;

@Service
public class UserService {

	@Autowired
	UserRepository userRepo;
	
	Logger logger = Logger.getGlobal();
	
	public CreateUserResponse createUser(CreateUserRequest request) {
		
		User user = new User(request.name(), request.mobNo());
		try {
			user.sha256();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Unable to generate token for user name:"+user.getName());
			e.printStackTrace();
		}
		
		user = userRepo.save(user);
		
		return ResponseBuilder.createUserResponse(request, null, null);
	}
	
}
