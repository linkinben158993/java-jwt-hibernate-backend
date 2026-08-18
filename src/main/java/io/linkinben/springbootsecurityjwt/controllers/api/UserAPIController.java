package io.linkinben.springbootsecurityjwt.controllers.api;

import java.security.Principal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import io.linkinben.springbootsecurityjwt.api.UsersApi;
import io.linkinben.springbootsecurityjwt.api.model.ChangePasswordRequest;
import io.linkinben.springbootsecurityjwt.api.model.MessageResponse;
import io.linkinben.springbootsecurityjwt.api.model.RegisterRequest;
import io.linkinben.springbootsecurityjwt.api.model.RegisterResponse;
import io.linkinben.springbootsecurityjwt.api.model.RoleAssignmentRequest;
import io.linkinben.springbootsecurityjwt.api.model.RoleAssignmentResponse;
import io.linkinben.springbootsecurityjwt.api.model.UserInfoRequest;
import io.linkinben.springbootsecurityjwt.api.model.UserProfileResponse;
import io.linkinben.springbootsecurityjwt.api.model.UserResponse;
import io.linkinben.springbootsecurityjwt.authz.CanEditUser;
import io.linkinben.springbootsecurityjwt.authz.UserAuthorizationService;
import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.dtos.UserInfoDTO;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.events.RoleAssignedEvent;
import io.linkinben.springbootsecurityjwt.events.UserRegisteredEvent;
import io.linkinben.springbootsecurityjwt.exceptions.DuplicateResourceException;
import io.linkinben.springbootsecurityjwt.exceptions.ForbiddenOperationException;
import io.linkinben.springbootsecurityjwt.exceptions.ResourceNotFoundException;
import io.linkinben.springbootsecurityjwt.services.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * Contract-first user endpoints — implements the generated {@link UsersApi} interface. Responses are
 * flat, typed DTOs (O-4b) and never expose the {@link Users} JPA entity (O-6). Request bodies bind the
 * generated request DTOs; the {@code uId} / principal is taken from the path or the security context,
 * never trusted from the body (mass-assignment / account-takeover guards).
 */
@Slf4j
@RestController
public class UserAPIController implements UsersApi {

	@Autowired
	private UserService userService;

	@Autowired
	private UserAuthorizationService authz;

	@Autowired
	private ApplicationEventPublisher publisher;

	@Override
	public ResponseEntity<UserProfileResponse> getCurrentUser(String xCorrelationId) {
		Users user = userService.findByEmail(currentPrincipalName());
		if (user == null) {
			throw new ResourceNotFoundException("User not found");
		}
		String role = firstRole(user);
		return ResponseEntity.ok(new UserProfileResponse(user.getEmail(), user.getFullName(), role));
	}

	@Override
	public ResponseEntity<List<UserResponse>> findAllUsers(String xCorrelationId) {
		List<UserResponse> users = userService.findAll().stream()
				.map(this::toUserResponse)
				.toList();
		return ResponseEntity.ok(users);
	}

	@Override
	public ResponseEntity<MessageResponse> findAllUsersWithoutRole(String xCorrelationId) {
		userService.editAllWithoutRole();
		return ResponseEntity.ok(new MessageResponse("All users updated!"));
	}

	@Override
	public ResponseEntity<MessageResponse> updateUsersRole(String xCorrelationId) {
		userService.editUsersRole();
		return ResponseEntity.ok(new MessageResponse("All users updated!"));
	}

	@Override
	public ResponseEntity<RegisterResponse> referAdmin(RegisterRequest registerRequest, String xCorrelationId) {
		if (userService.findByEmail(registerRequest.getEmail()) != null) {
			throw new DuplicateResourceException("Email has already been used!");
		}
		userService.add(toUser(registerRequest), "ROLE_ADMIN");
		publisher.publishEvent(new UserRegisteredEvent(registerRequest.getEmail(), "ROLE_ADMIN"));
		return ResponseEntity.ok(new RegisterResponse(registerRequest.getEmail()));
	}

	@Override
	public ResponseEntity<RegisterResponse> register(RegisterRequest registerRequest, String xCorrelationId) {
		if (userService.findByEmail(registerRequest.getEmail()) != null) {
			throw new DuplicateResourceException("Email has already been used!");
		}
		userService.add(toUser(registerRequest), "ROLE_USER");
		publisher.publishEvent(new UserRegisteredEvent(registerRequest.getEmail(), "ROLE_USER"));
		return ResponseEntity.ok(new RegisterResponse(registerRequest.getEmail()));
	}

	@Override
	public ResponseEntity<MessageResponse> changePassword(ChangePasswordRequest changePasswordRequest,
			String xCorrelationId) {
		// Ownership: always target the authenticated principal — never a body-supplied email (account takeover).
		ChangePasswordDTO dto = new ChangePasswordDTO(currentPrincipalName(), changePasswordRequest.getPassword());
		userService.editPassword(dto);
		return ResponseEntity.ok(new MessageResponse("Password changed!"));
	}

	@Override
	public ResponseEntity<UserResponse> updateInfo(UserInfoRequest userInfoRequest, String xCorrelationId) {
		Users current = userService.findByEmail(currentPrincipalName());
		if (current == null) {
			throw new ResourceNotFoundException("User not found");
		}
		// Principal is the current user extracted from the token — may only edit their own record here.
		UserInfoDTO dto = toUserInfoDTO(current.getuId(), userInfoRequest);
		userService.edit(dto);

		UserResponse response = new UserResponse(
				current.getuId(), current.getEmail(), userInfoRequest.getFullName(), firstRole(current));
		response.setAge(userInfoRequest.getAge());
		response.setDob(userInfoRequest.getDob());
		return ResponseEntity.ok(response);
	}

	// Edit another user's profile — ownership/rank enforced by @CanEditUser (self or higher rank).
	@CanEditUser
	@Override
	public ResponseEntity<UserResponse> updateUserById(String id, UserInfoRequest userInfoRequest,
			String xCorrelationId) {
		UserInfoDTO dto = toUserInfoDTO(id, userInfoRequest); // path id wins (closes mass-assignment, G15)
		userService.edit(dto);

		Users target = userService.findById(id);
		String email = target != null ? target.getEmail() : null;
		String role = target != null ? firstRole(target) : null;
		UserResponse response = new UserResponse(id, email, userInfoRequest.getFullName(), role);
		response.setAge(userInfoRequest.getAge());
		response.setDob(userInfoRequest.getDob());
		return ResponseEntity.ok(response);
	}

	// Assign a role. @CanEditUser gates access to the target (404 if not allowed); the granted-role
	// rule is a separate guard producing 403 (target visible, only the action forbidden).
	@CanEditUser
	@Override
	public ResponseEntity<RoleAssignmentResponse> assignRole(String id, RoleAssignmentRequest roleAssignmentRequest,
			String xCorrelationId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String role = roleAssignmentRequest.getRole();
		if (!authz.canAssignRole(authentication, role)) {
			throw new ForbiddenOperationException("Cannot grant a role at or above your own rank");
		}
		userService.assignRole(id, role);
		publisher.publishEvent(new RoleAssignedEvent(authentication.getName(), id, role));
		return ResponseEntity.ok(new RoleAssignmentResponse(id, role));
	}

	// Delete a user — same ownership/rank gate as edit.
	@CanEditUser
	@Override
	public ResponseEntity<MessageResponse> deleteUser(String id, String xCorrelationId) {
		userService.delete(id);
		return ResponseEntity.ok(new MessageResponse("User deleted!"));
	}

	// D6: an ownership/rank denial (AuthorizationDeniedException from @CanEditUser) is hidden as 404,
	// indistinguishable from a missing resource. Controller-local override of the global 403 handler.
	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<?> handleAuthorizationDenied() {
		return ResponseEntity.notFound().build();
	}

	private String currentPrincipalName() {
		Principal principal = SecurityContextHolder.getContext().getAuthentication();
		return principal.getName();
	}

	private String firstRole(Users user) {
		if (user.getRoles() == null) {
			return "NO_ROLE";
		}
		return user.getRoles().stream().map(Roles::getrName).findFirst().orElse("NO_ROLE");
	}

	private UserResponse toUserResponse(Users user) {
		UserResponse response = new UserResponse(
				user.getuId(), user.getEmail(), user.getFullName(), firstRole(user));
		response.setAge(user.getAge());
		response.setDob(user.getDob() != null ? user.getDob().toLocalDate() : null);
		return response;
	}

	/** Maps the register request to a Users entity carrying only the three client fields (no id, no roles). */
	private Users toUser(RegisterRequest request) {
		Users user = new Users();
		user.setEmail(request.getEmail());
		user.setFullName(request.getFullName());
		user.setPassword(request.getPassword());
		return user;
	}

	private UserInfoDTO toUserInfoDTO(String uId, UserInfoRequest request) {
		UserInfoDTO dto = new UserInfoDTO();
		dto.setuId(uId);
		dto.setFullName(request.getFullName());
		dto.setAge(request.getAge());
		LocalDate dob = request.getDob();
		dto.setDob(dob != null ? Date.valueOf(dob) : null);
		return dto;
	}
}
