package io.linkinben.springbootsecurityjwt.repositories;

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.entities.Users;

public interface UserRepository extends GenericRepository<Users, String>{
	void update(Users user);
	int updatePassword(ChangePasswordDTO changePasswordDTO);
	Users findByEmail(String email);
}
