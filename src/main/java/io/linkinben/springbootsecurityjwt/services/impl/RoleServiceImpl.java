package io.linkinben.springbootsecurityjwt.services.impl;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.repositories.RoleRepository;
import io.linkinben.springbootsecurityjwt.services.RoleService;

@Slf4j
@Service
public class RoleServiceImpl extends GenericServiceImpl<Roles, String> implements RoleService {

	@Autowired
	private RoleRepository roleRepository;

	@Override
	public void add(Roles role) {
		try {
			String id = UUID.randomUUID().toString();
			role.setrId(id);
			roleRepository.insert(role);
		} catch (Exception e) {
			log.error("Failed to insert role: {}", role.getrName(), e);
			throw e;
		}
	}

	@Override
	public void edit(Roles role) {
		roleRepository.update(role);
	}

	@Override
	public Roles findByRoleName(String rName) {
		return roleRepository.findByRoleName(rName);
	}
}
