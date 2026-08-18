package io.linkinben.springbootsecurityjwt.controllers;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.linkinben.springbootsecurityjwt.api.SystemApi;
import io.linkinben.springbootsecurityjwt.api.model.WelcomeResponse;

/**
 * Contract-first System endpoint — implements the generated {@link SystemApi} interface. Returns a flat,
 * typed {@link WelcomeResponse}.
 */
@Slf4j
@RestController
public class HomeController implements SystemApi {
	private final List<String> welcomeString;

	public HomeController() {
		List<String> welcomeMessage = new ArrayList<String>();
		welcomeMessage.add("Hello!");
		welcomeMessage.add("First re-visit Spring boot!");
		this.welcomeString = welcomeMessage;
	}

	@Override
	public ResponseEntity<WelcomeResponse> helloWorld(String xCorrelationId) {
		WelcomeResponse response = new WelcomeResponse();
		response.setTitle(this.welcomeString.get(0));
		response.setMessage(this.welcomeString.get(1));
		return ResponseEntity.ok(response);
	}
}
