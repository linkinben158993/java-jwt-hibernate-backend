package io.linkinben.springbootsecurityjwt.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.repositories.RoleRepository;
import io.linkinben.springbootsecurityjwt.services.RoleService;

@Service
public class RoleServiceImpl extends GenericServiceImpl<Roles, String> implements RoleService {

	@Autowired
	private RoleRepository roleRepository;

	@Override
	public void edit(Roles role) {
		roleRepository.update(role);
	}
}
