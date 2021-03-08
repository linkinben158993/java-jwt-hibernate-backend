package io.linkinben.springbootsecurityjwt.repositories.impl;

import javax.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.repositories.RoleRepository;

@Repository
@Transactional(rollbackOn = Exception.class)
public class RoleRepositoryImpl extends GenericRepositoryImpl<Roles, String> implements RoleRepository{

	@Override
	public void update(Roles role) {
		
	}

	@Override
	public Roles findByRoleName(String rName) {
		String hql = "FROM roles where rName = :rName";
		try {
			Session session = sessionFactory.getCurrentSession();
			Query<Roles> query = session.createQuery(hql, Roles.class);
			query.setParameter("rName", rName);
			return query.getSingleResult();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
