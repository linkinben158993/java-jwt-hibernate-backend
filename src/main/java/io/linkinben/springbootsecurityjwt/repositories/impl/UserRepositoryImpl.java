package io.linkinben.springbootsecurityjwt.repositories.impl;

import java.beans.IntrospectionException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.transaction.Transactional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.dtos.UserInfoDTO;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.UserRepository;

@Repository
@Transactional(rollbackOn = Exception.class)
public class UserRepositoryImpl extends GenericRepositoryImpl<Users, String> implements UserRepository {

	@PersistenceContext
	protected EntityManager entityManager;

	@Override
	public void update(UserInfoDTO user) {
		Session session = sessionFactory.getCurrentSession();
		Users foundUser = this.findById(user.getuId());
		foundUser.setFullName(user.getFullName() != null ? user.getFullName() : foundUser.getFullName());
		foundUser.setAge(user.getAge()  != null ? user.getAge() : foundUser.getAge());
		foundUser.setDob(user.getDob()  != null ? user.getDob() : foundUser.getDob());
		try {
			session.update(foundUser);
		} catch (HibernateException e) {
			e.printStackTrace();
		}

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
