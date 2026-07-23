package io.linkinben.springbootsecurityjwt.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Slf4j
@RestController
@RequestMapping("home")
public class HomeController {
	private List<String> welcomeString;

	public HomeController() {
		List<String> welcomeMessage = new ArrayList<String>();
		welcomeMessage.add("Hello!");
		welcomeMessage.add("First re-visit Spring boot!");
		this.welcomeString = welcomeMessage;
	}

	@RequestMapping(value = "/hello-world", method = RequestMethod.GET)
	public ResponseEntity<Object> Welcome() {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response.put("title", this.welcomeString.get(0));
			response.put("message", this.welcomeString.get(1));
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			log.error("GET /home/helloWorld failed", e);
			return new ResponseEntity<Object>("Something happened!", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
