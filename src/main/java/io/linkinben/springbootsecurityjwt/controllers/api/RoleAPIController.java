package io.linkinben.springbootsecurityjwt.controllers.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.linkinben.springbootsecurityjwt.api.RolesApi;
import io.linkinben.springbootsecurityjwt.api.model.CreateRoleRequest;
import io.linkinben.springbootsecurityjwt.api.model.RoleResponse;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.services.RoleService;

/**
 * Contract-first role endpoint — implements the generated {@link RolesApi} interface. Under O-6 / S-3
 * the request body is a typed {@link CreateRoleRequest}; the raw {@link Roles} JPA entity is no longer
 * accepted (it exposed the users back-reference). The response is a flat {@link RoleResponse}.
 */
@RestController
public class RoleAPIController implements RolesApi {

	@Autowired
	private RoleService roleService;

	@Override
	public ResponseEntity<RoleResponse> createRole(CreateRoleRequest createRoleRequest, String xCorrelationId) {
		Roles role = new Roles();
		role.setrName(createRoleRequest.getrName());
		roleService.add(role);

		RoleResponse response = new RoleResponse(role.getrName());
		response.setrId(role.getrId());
		return ResponseEntity.ok(response);
	}
}
