package io.linkinben.springbootsecurityjwt.controllers.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.linkinben.springbootsecurityjwt.entities.Roles;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.linkinben.springbootsecurityjwt.authz.CanEditUser;
import io.linkinben.springbootsecurityjwt.authz.UserAuthorizationService;
import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.dtos.RegisterRequest;
import io.linkinben.springbootsecurityjwt.dtos.UserInfoDTO;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.exceptions.DuplicateResourceException;
import io.linkinben.springbootsecurityjwt.exceptions.ForbiddenOperationException;
import io.linkinben.springbootsecurityjwt.exceptions.ResourceNotFoundException;
import io.linkinben.springbootsecurityjwt.services.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/users")
public class UserAPIController {

	@Autowired
	private UserService userService;

	@Autowired
	private UserAuthorizationService authz;

	@RequestMapping(value = "/me", method = RequestMethod.GET)
	public ResponseEntity<?> getCurrentUser(Principal principal) {
		Users user = userService.findByEmail(principal.getName());
		if (user == null) {
			throw new ResourceNotFoundException("User not found");
		}
		String role = user.getRoles().stream().map(Roles::getrName).findFirst().orElse("NO_ROLE");
		Map<String, Object> data = new HashMap<>();
		data.put("email", user.getEmail());
		data.put("fullName", user.getFullName());
		data.put("role", role);
		return ok("User profile", "Profile retrieved", data);
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<?> findAllUsers() {
		List<Users> users = userService.findAll();
		return ok("Request for all users.", "All users found!", users);
	}

	@RequestMapping(value = "/without-role", method = RequestMethod.GET)
	public ResponseEntity<?> findAllUsersWithoutRole() {
		userService.editAllWithoutRole();
		return ok("Update role.", "All users updated!", "Whatsup");
	}

	@RequestMapping(value = "/roles", method = RequestMethod.GET)
	public ResponseEntity<?> updateUserWithoutRole() {
		userService.editUsersRole();
		return ok("Update role.", "All users updated!", "Whatsup");
	}

	@RequestMapping(value = "/admin", method = RequestMethod.POST)
	public ResponseEntity<?> referAdmin(@Valid @RequestBody RegisterRequest request) {
		if (userService.findByEmail(request.getEmail()) != null) {
			throw new DuplicateResourceException("Email has already been used!");
		}
		userService.add(request.toUser(), "ROLE_ADMIN");
		return ok("Create new admin user.", "New admin user add!", request.getEmail());
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
		if (userService.findByEmail(request.getEmail()) != null) {
			throw new DuplicateResourceException("Email has already been used!");
		}
		userService.add(request.toUser(), "ROLE_USER");
		return ok("Create new user.", "New user created!", request.getEmail());
	}

	@RequestMapping(value = "/password", method = RequestMethod.PATCH)
	public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDTO user, Principal principal) {
		// Ownership: always target the authenticated principal — never the body email (account takeover).
		user.setEmail(principal.getName());
		userService.editPassword(user);
		return ok("Change password for: " + user.getEmail(), "Password changed!", user);
	}

	// Edit another user's profile — ownership/rank enforced by @CanEditUser (self or higher rank).
	@CanEditUser
	@RequestMapping(value = "/{id}", method = RequestMethod.PATCH)
	public ResponseEntity<?> updateUserById(@PathVariable String id, @Valid @RequestBody UserInfoDTO user) {
		user.setuId(id); // path id wins over any body-supplied uId (closes mass-assignment, G15)
		userService.edit(user);
		return ok("Update user", "User updated!", id);
	}

	// Assign a role. @CanEditUser gates access to the target (404 if not allowed); the granted-role
	// rule is a separate guard producing 403 (target visible, only the action forbidden).
	@CanEditUser
	@RequestMapping(value = "/{id}/role", method = RequestMethod.PATCH)
	public ResponseEntity<?> assignRole(@PathVariable String id, @RequestBody Map<String, String> body,
			Authentication authentication) {
		String role = body.get("role");
		if (!authz.canAssignRole(authentication, role)) {
			throw new ForbiddenOperationException("Cannot grant a role at or above your own rank");
		}
		userService.assignRole(id, role);
		Map<String, Object> data = new HashMap<>();
		data.put("uId", id);
		data.put("role", role);
		return ok("Assign role", "Role assigned!", data);
	}

	// Delete a user — same ownership/rank gate as edit.
	@CanEditUser
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteUser(@PathVariable String id) {
		userService.delete(id);
		return ok("Delete user", "User deleted!", id);
	}

	@RequestMapping(value = "/info", method = RequestMethod.PATCH)
	public ResponseEntity<?> updateInfo(@Valid @RequestBody UserInfoDTO user, Principal principal) {
		Users current = userService.findByEmail(principal.getName());
		if (current == null) {
			throw new ResourceNotFoundException("User not found");
		}
		// Principal is current user extracted from token — may only edit their own record here.
		if (!current.getuId().equals(user.getuId())) {
			throw new ForbiddenOperationException("You are not authorized to edit this user!");
		}
		userService.edit(user);
		return ok("Request Change Info For: " + user.getFullName(), "Info changed!", user);
	}

	// D6: an ownership/rank denial (AuthorizationDeniedException from @CanEditUser) is hidden as 404,
	// indistinguishable from a missing resource. Controller-local override of the global 403 handler.
	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<?> handleAuthorizationDenied() {
		return ResponseEntity.notFound().build();
	}

	private ResponseEntity<Object> ok(String title, String message, Object data) {
		Map<String, Object> response = new HashMap<>();
		response.put("title", title);
		response.put("message", message);
		response.put("data", data);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
