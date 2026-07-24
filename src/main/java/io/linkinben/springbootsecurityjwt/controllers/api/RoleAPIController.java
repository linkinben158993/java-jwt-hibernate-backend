package io.linkinben.springbootsecurityjwt.controllers.api;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.services.RoleService;

@RestController
@RequestMapping("api/roles")
public class RoleAPIController {

	@Autowired
	private RoleService roleService;

	@RequestMapping(value = "", method = RequestMethod.POST)
	public ResponseEntity<?> create(@Valid @RequestBody Roles role) {
		roleService.add(role);
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("title", "Create new role.");
		response.put("message", "New role created!");
		response.put("data", role.getrName());
		return new ResponseEntity<Object>(response, HttpStatus.OK);
	}
}
