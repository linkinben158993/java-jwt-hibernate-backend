package io.linkinben.springbootsecurityjwt.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.linkinben.springbootsecurityjwt.dtos.AuthenticationResponse;
import springfox.documentation.schema.Model;

@Controller
@RequestMapping("oauth")
public class ThirdPartyController {

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;
	
	@RequestMapping(value = "/register-google", method = RequestMethod.GET)
	public String registerWithOAuth2() {
		System.out.println("Ayo whatssup!");
		return "Ayo whatssup!";
	}

	@RequestMapping(value = "/register-google/success", method = RequestMethod.GET)
	public ResponseEntity<?> registerWithOAuth2Success(Model model, OAuth2AuthenticationToken authToken) {
		System.out.println("Name: " + authToken.getName());
		System.out.println("Credential: " + authToken.getName());
		OAuth2AuthorizedClient client = authorizedClientService
				.loadAuthorizedClient(authToken.getAuthorizedClientRegistrationId(), authToken.getName());

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("title", "Good Credential!");
		response.put("message", "Access Granted!");
		response.put("data", client);
		AuthenticationResponse authenticationResponse = new AuthenticationResponse(response);
		return new ResponseEntity<Object>(authenticationResponse, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/register-google/fail", method = RequestMethod.GET)
	public ResponseEntity<?> registerWithOAuth2Fail(Model model) {
		System.out.println("Ayo whatssup from failed!");
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("title", "Bad!");
		response.put("message", "Access Denied!");
		response.put("data", "Saed");
		AuthenticationResponse authenticationResponse = new AuthenticationResponse(response);
		return new ResponseEntity<Object>(authenticationResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
