package io.linkinben.springbootsecurityjwt.services.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.UserRepository;
import io.linkinben.springbootsecurityjwt.services.UserService;

@Service
public class UserServiceImpl extends GenericServiceImpl<Users, String> implements UserService {
	@Autowired
	protected UserRepository userRepository;

	@Override
	public void add(Users user) {
		String id = UUID.randomUUID().toString();
		user.setuId(id);
		String hashed = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
		user.setPassword(hashed);
		userRepository.insert(user);
	}

	@Override
	public List<Users> findAll() {
		return userRepository.findAll();
	}

	@Override
	public Users findById(String id) {
		return userRepository.findById(id);
	}
	
	@Override
	public Users findByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	@Override
	public void edit(Users user) {
		userRepository.update(user);
	}

	@Override
	public int editPassword(ChangePasswordDTO changePasswordDTO) {
		String hashed = BCrypt.hashpw(changePasswordDTO.getPassword(), BCrypt.gensalt(12));
		changePasswordDTO.setPassword(hashed);
		return userRepository.updatePassword(changePasswordDTO);
	}

	@Override
	public int delete(String id) {
		if (userRepository.removeById(id) == 1) {
			return 1;
		}
		return 0;
	}
}
