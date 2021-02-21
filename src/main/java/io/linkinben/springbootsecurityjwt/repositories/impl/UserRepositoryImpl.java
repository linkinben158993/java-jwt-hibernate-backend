package io.linkinben.springbootsecurityjwt.repositories.impl;

import javax.transaction.Transactional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.UserRepository;

@Repository
@Transactional(rollbackOn = Exception.class)
public class UserRepositoryImpl extends GenericRepositoryImpl<Users, String> implements UserRepository{

	@Override
	public void update(Users user) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int updatePassword(ChangePasswordDTO changePasswordDTO) {
		Session session = sessionFactory.getCurrentSession();
		String hql = "UPDATE users SET " + "password = :password " + "WHERE email = :email";
		try {
			Query query = session.createQuery(hql);
			query.setParameter("password", changePasswordDTO.getPassword());
			query.setParameter("email", changePasswordDTO.getEmail());

			return query.executeUpdate();
		} catch (HibernateException e) {
			e.printStackTrace();
			return -1;
		}
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
