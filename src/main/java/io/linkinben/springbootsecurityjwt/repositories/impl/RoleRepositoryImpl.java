package io.linkinben.springbootsecurityjwt.repositories.impl;

import javax.transaction.Transactional;

import org.springframework.stereotype.Repository;

import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.repositories.RoleRepository;

@Repository
@Transactional(rollbackOn = Exception.class)
public class RoleRepositoryImpl extends GenericRepositoryImpl<Roles, String> implements RoleRepository{

	@Override
	public void update(Roles role) {
		
	}

}
