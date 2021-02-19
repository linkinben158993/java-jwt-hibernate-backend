package io.linkinben.springbootsecurityjwt.repositories.impl;

import javax.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.UserRepository;

@Repository
@Transactional(rollbackOn = Exception.class)
public class UserRepositoryImpl extends GenericRepositoryImpl<Users, String> implements UserRepository{

	@Override
	public void update(Users user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int updatePassword(String user) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Users findByEmail(String email) {
		String hql = "FROM users where email = :email";
		try {
			Session session = sessionFactory.getCurrentSession();
			Query<Users> query = session.createQuery(hql, Users.class);
			query.setParameter("email", email);
			return query.getSingleResult();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
