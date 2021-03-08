package io.linkinben.springbootsecurityjwt.controllers.api;

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

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.services.RoleService;
import io.linkinben.springbootsecurityjwt.services.UserService;

@Controller
@RequestMapping("api/user")
public class UserAPIController {

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
			Roles role = roleService.findByRoleName("ROLE_USER");
			System.out.println(role.getrId());
			System.out.println(role.getrName());
			ownedRoles.add(role);
			user.setRoles(ownedRoles);
			userService.add(user);
			System.out.println(user.getEmail());
			System.out.println(user.getFullName());
			System.out.println(user.getPassword());
			response.put("title", "Create new user.");
			response.put("message", "New user created!");
			response.put("data", user.getEmail());
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			response.put("title", "Request for a new account.");
			response.put("message", "Something happened!");
			response.put("data", "Bad request!");
			return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/change-password", method = RequestMethod.POST)
	public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDTO user) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			System.out.println(user.getEmail());
			userService.editPassword(user);
			response.put("title", "Change password for: " + user.getEmail());
			response.put("message", "Password changed!");
			response.put("data", user);
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			response.put("title", "Request change password for: " + user.getEmail());
			response.put("message", "Something happened!");
			response.put("data", "Bad request!");
			return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
		}
	}
}
