package io.linkinben.springbootsecurityjwt.repositories.impl;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import io.linkinben.springbootsecurityjwt.repositories.GenericRepository;

@Repository
@Transactional(rollbackOn = Exception.class)
public abstract class GenericRepositoryImpl<T, K extends Serializable> implements GenericRepository<T, K> {

    @PersistenceContext
    protected EntityManager entityManager;

    protected Class<? extends T> clazz;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GenericRepositoryImpl() {
        Type type = getClass().getGenericSuperclass();
        ParameterizedType paramType = (ParameterizedType) type;
        clazz = (Class) paramType.getActualTypeArguments()[0];
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<T> findAll() {
        Session session = entityManager.unwrap(Session.class);
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
        Session session = entityManager.unwrap(Session.class);
        try {
            return session.find(clazz, id);
        } catch (HibernateException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void insert(T object) {
        Session session = entityManager.unwrap(Session.class);
        try {
            session.merge(object);
        } catch (HibernateException e) {
            e.printStackTrace();
        }
    }

    public int removeById(K id) {
        Session session = entityManager.unwrap(Session.class);
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
