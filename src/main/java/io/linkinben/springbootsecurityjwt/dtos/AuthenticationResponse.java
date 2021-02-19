package io.linkinben.springbootsecurityjwt.dtos;

import java.io.Serializable;
import java.util.Map;

public class AuthenticationResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	// private final String JWToken;
	// Response should comprise of title, message, jwt and token on login
	private final Map<String, Object> response;

	public AuthenticationResponse(Map<String, Object> response) {
		this.response = response;
	}

	public Map<String, Object> getResponse(){
		return response;
	}
}
