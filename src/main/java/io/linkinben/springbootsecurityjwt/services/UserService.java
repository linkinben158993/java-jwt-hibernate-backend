package io.linkinben.springbootsecurityjwt.services;

import java.util.List;

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.entities.Users;

public interface UserService extends GenericService<Users, String> {

	List<Users> findAll();

	// Had to override add because adding user requires hashing of password
	void add(Users user);
	
	
	// Edit user 
	void edit(Users user);

	int editPassword(ChangePasswordDTO changePasswordDTO);

}
