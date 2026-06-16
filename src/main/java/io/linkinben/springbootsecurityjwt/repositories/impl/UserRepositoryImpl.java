package io.linkinben.springbootsecurityjwt.repositories.impl;

import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.dtos.UserInfoDTO;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.UserRepository;

@Repository
@Transactional(rollbackOn = Exception.class)
public class UserRepositoryImpl extends GenericRepositoryImpl<Users, String> implements UserRepository {
    Logger logger = LoggerFactory.getLogger(UserRepositoryImpl.class);

	@PersistenceContext
	protected EntityManager entityManager;

	@Override
	public void update(UserInfoDTO user) {
		Session session = entityManager.unwrap(Session.class);
		Users foundUser = this.findById(user.getuId());
		foundUser.setFullName(user.getFullName() != null ? user.getFullName() : foundUser.getFullName());
		foundUser.setAge(user.getAge() != null ? user.getAge() : foundUser.getAge());
		foundUser.setDob(user.getDob() != null ? user.getDob() : foundUser.getDob());
		try {
			session.merge(foundUser);
		} catch (HibernateException e) {
			e.printStackTrace();
		}

	}

	@Override
	public int updatePassword(ChangePasswordDTO changePasswordDTO) {
		Session session = entityManager.unwrap(Session.class);
		String hql = "UPDATE users SET " + "password = :password " + "WHERE email = :email";
		try {
			Query<Users> query = session.createQuery(hql, Users.class);
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
			Session session = entityManager.unwrap(Session.class);
			Query<Users> query = session.createQuery(hql, Users.class);
			query.setParameter("email", email);
			return query.getResultList().stream().findFirst().orElse(null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void batchUpdateUserRoleCriteria(Set<Roles> roles) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Users> cqUser = cb.createQuery(Users.class);

		Root<Users> rootQueryUsers = cqUser.from(Users.class);
		Predicate roleIsEmpty = cb.isEmpty(rootQueryUsers.get("roles"));

		// Select
		cqUser.where(roleIsEmpty);
		TypedQuery<Users> queryUserWithoutRole = entityManager.createQuery(cqUser.select(rootQueryUsers));
		List<Users> foundUsers = queryUserWithoutRole.getResultList();
		logger.info("Found Users: " + foundUsers.size());

		// Update
		
		// More research here
//		CriteriaUpdate<Users> cuUsers = cb.createCriteriaUpdate(Users.class);
//		// Set Root Update Class
//		Root<Users> rootUpdateUsers = cuUsers.from(Users.class);
//
//		cuUsers.set("roles", roles);
//		cuUsers.where(cb.isEmpty(rootUpdateUsers.get("roles")));
//		int result = entityManager.createQuery(cuUsers).executeUpdate();
//		System.out.println("Update Result: " + result);

		Session session = entityManager.unwrap(Session.class);
		for (Users item : foundUsers) {
			item.setRoles(roles);
		}
		try {
			for (Users item : foundUsers) { session.merge(item); }
		} catch (HibernateException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void batchUpdateUserRoleHQL(Set<Roles> roles) {
		Session session = entityManager.unwrap(Session.class);
//		Or This:
//		String hql = "FROM users u left join u.roles r where r.rId is null";
		String hql = "SELECT u FROM users u LEFT JOIN u.roles r WHERE r.rId IS NULL";
		try {
			Query<Users> query = session.createQuery(hql, Users.class);
			List<Users> foundUsers = query.getResultList();
			logger.info("Update user size: " + foundUsers.size());
			for (Users item : foundUsers) {
				item.setRoles(roles);
			}
			try {
				for (Users item : foundUsers) { session.merge(item); }
			} catch (HibernateException e) {
				e.printStackTrace();
			}
		} catch (HibernateException e) {
			e.printStackTrace();
		}
	}

}
