package io.linkinben.springbootsecurityjwt.repositories.impl;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.linkinben.springbootsecurityjwt.repositories.GenericRepository;

@Repository
@Transactional(rollbackOn = Exception.class)
public abstract class GenericRepositoryImpl<T, K extends Serializable> implements GenericRepository<T, K> {
	@Autowired
	protected SessionFactory sessionFactory;
	
	protected Class<? extends T> clazz;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public GenericRepositoryImpl() {
		Type type = getClass().getGenericSuperclass();
		ParameterizedType paramType = (ParameterizedType) type;
		clazz = (Class) paramType.getActualTypeArguments()[0];
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<T> findAll() {
		Session session = sessionFactory.getCurrentSession();
		try {
			CriteriaBuilder builder = session.getCriteriaBuilder();
			
			CriteriaQuery query = (CriteriaQuery) builder.createQuery(clazz);
			
			Root<T> root = (Root<T>) query.from(clazz);
			
			query.select(root);
			
			Query<T> q = session.createQuery(query);
			
			return q.getResultList();
			
		} catch (HibernateException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public T findById(K id) {
		Session session = sessionFactory.getCurrentSession();
		try {
			return session.find(clazz, id);
		} catch (HibernateException e) {
			e.printStackTrace();
		} 
		return null;
	}
	
	public void insert(T object) {
		Session session = sessionFactory.getCurrentSession();
		try {
			session.saveOrUpdate(object);
		} catch (HibernateException e) {
			e.printStackTrace();
		} 
	}
	
	public int removeById(K id) {
		Session session = sessionFactory.getCurrentSession();
		try {
			T object = findById(id);
			session.remove(object);
		} catch (HibernateException e) {
			e.printStackTrace();
			return 0;

		}
		return 1;
	}
}
