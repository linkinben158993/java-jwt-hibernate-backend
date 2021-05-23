package io.linkinben.springbootsecurityjwt.services;

import java.util.List;

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.dtos.UserInfoDTO;
import io.linkinben.springbootsecurityjwt.entities.Users;

public interface UserService extends GenericService<Users, String> {

	List<Users> findAll();
	
	Users findByEmail(String email);

	// Had to override add because adding user requires hashing of password
	void add(Users user, String roleName);

	// Edit user
	void edit(UserInfoDTO user);

	void editUsersRole();
	
	void editAllWithoutRole();

	int editPassword(ChangePasswordDTO changePasswordDTO);

}
