package io.linkinben.springbootsecurityjwt.services;

import java.util.List;

import io.linkinben.springbootsecurityjwt.entities.Roles;

public interface RoleService extends GenericService<Roles, String> {
	List<Roles> findAll();

	Roles findByRoleName(String rName);
	
	void add(Roles role);

	void edit(Roles role);
}
