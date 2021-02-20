package io.linkinben.springbootsecurityjwt.repositories;

import io.linkinben.springbootsecurityjwt.entities.Roles;

public interface RoleRepository extends GenericRepository<Roles, String>{
	void update(Roles role);
}
