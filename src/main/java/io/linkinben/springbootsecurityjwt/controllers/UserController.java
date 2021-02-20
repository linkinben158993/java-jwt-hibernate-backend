package io.linkinben.springbootsecurityjwt.controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.services.RoleService;
import io.linkinben.springbootsecurityjwt.services.UserService;

@Controller
@RequestMapping("user")
public class UserController {

	@Autowired
	private UserService userService;
	
	@Autowired
	private RoleService roleService;

	@RequestMapping(value = "", method = RequestMethod.GET)
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

	@RequestMapping(value = "/register", method = RequestMethod.POST)
	public ResponseEntity<?> register(@RequestBody Users user) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			Set<Roles> ownedRoles = new HashSet<Roles>();
			Roles role = roleService.findById("2");
			System.out.println(role.getrName());
			ownedRoles.add(role);
			user.setRoles(ownedRoles);
			userService.add(user);
			response.put("title", "Create new user.");
			response.put("message", "New user create!");
			response.put("data", user);
			return new ResponseEntity<Object>(response, HttpStatus.OK);			
		} catch (Exception e) {
			response.put("title", "Request for a new account.");
			response.put("message", "Something happened!");
			response.put("data", "Bad request!");
			return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
		}

	}
}
