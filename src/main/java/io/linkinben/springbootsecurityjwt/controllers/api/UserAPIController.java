package io.linkinben.springbootsecurityjwt.controllers.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.dtos.UserInfoDTO;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.services.UserService;
import io.linkinben.springbootsecurityjwt.utils.EmailUtils;
import io.swagger.annotations.ApiImplicitParam;

@Controller
@RequestMapping("api/user")
public class UserAPIController {

	@Autowired
	private UserService userService;
	
	@Autowired EmailUtils emailUtils;

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ApiImplicitParam(name = "access_token", required = true, allowEmptyValue = false, paramType = "header", dataTypeClass = String.class, example = "Bearer access_token")
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
	
	@RequestMapping(value = "/update-role", method = RequestMethod.GET)
	@ApiImplicitParam(name = "access_token", required = true, allowEmptyValue = false, paramType = "header", dataTypeClass = String.class, example = "Bearer access_token")
	public ResponseEntity<?> updateUserWithoutRole() {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			userService.editUsersRole();
			response.put("title", "Update role.");
			response.put("message", "All users updated!");
			response.put("data", "Whatsup");
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			response.put("title", "Request update users role.");
			response.put("message", "Something happened!");
			response.put("data", "Bad request!");
			return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/register", method = RequestMethod.POST)
	public ResponseEntity<?> register(@RequestBody Users user) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			userService.add(user);
			response.put("title", "Create new user.");
			response.put("message", "New user created!");
			response.put("data", user.getEmail());
			emailUtils.sendSimpleEmail(user.getEmail(), "Whatssup Mother Fucker!");
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			response.put("title", "Request for a new account.");
			response.put("message", "Something happened!");
			response.put("data", "Bad request!");
			e.printStackTrace();
			return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/change-password", method = RequestMethod.POST)
	public ResponseEntity<?> changePassword(@RequestHeader(value = "access_token") String access_token, @RequestBody ChangePasswordDTO user) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
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

	@RequestMapping(value = "/update-info", method = RequestMethod.POST)
	@ApiImplicitParam(name = "access_token", required = true, allowEmptyValue = false, paramType = "header", dataTypeClass = String.class, example = "Bearer access_token")
	public ResponseEntity<?> updateInfo(@RequestBody UserInfoDTO user, Principal principal) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			// Principal is current user extract from token
			if (!userService.findByEmail(principal.getName()).getuId().equals(user.getuId())) {
				response.put("title", "Request Change Info For: " + user.getFullName());
				response.put("message", "You are not authorized to edit this user!");
				response.put("data", "Forbidden!");
				return new ResponseEntity<Object>(response, HttpStatus.FORBIDDEN);
			} else {
				userService.edit(user);
				response.put("title", "Request Change Info For: " + user.getFullName());
				response.put("message", "Info changed!");
				response.put("data", user);
				return new ResponseEntity<Object>(response, HttpStatus.OK);
			}
		} catch (Exception e) {
			response.put("title", "Request Change Info For: " + user.getFullName());
			response.put("message", "Something happened!");
			response.put("data", "Bad request!");
			return new ResponseEntity<Object>(response, HttpStatus.BAD_REQUEST);
		}
	}
}
