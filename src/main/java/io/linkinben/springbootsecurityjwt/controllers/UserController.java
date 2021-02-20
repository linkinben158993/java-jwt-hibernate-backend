package io.linkinben.springbootsecurityjwt.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.services.UserService;

@Controller
@RequestMapping("user")
public class UserController {

	@Autowired
	private UserService userService;

	@GetMapping("")
	public ResponseEntity<?> findAllUsers() {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			List<Users> users = userService.findAll();
			response.put("title", "Request for all users.");
			response.put("message", "All users found!");
			response.put("data", users);
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			response.put("title", "Request for all users.");
			response.put("message", "Something happened!");
			response.put("data", "Bad request!");
			return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
		}
	}
}
