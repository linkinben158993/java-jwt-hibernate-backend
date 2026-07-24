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

    // Propagate instead of swallow: DB/Hibernate errors bubble up to GlobalExceptionHandler (500)
    // rather than being hidden as null/0. Legitimate "not found" is still a null return from find().
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<T> findAll() {
        Session session = entityManager.unwrap(Session.class);
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery query = (CriteriaQuery) builder.createQuery(clazz);
        Root<T> root = (Root<T>) query.from(clazz);
        query.select(root);
        Query<T> q = session.createQuery(query);
        return q.getResultList();
    }

    public T findById(K id) {
        Session session = entityManager.unwrap(Session.class);
        return session.find(clazz, id);
    }

    public void insert(T object) {
        Session session = entityManager.unwrap(Session.class);
        session.merge(object);
    }

    public int removeById(K id) {
        Session session = entityManager.unwrap(Session.class);
        T object = findById(id);
        if (object == null) {
            return 0; // nothing to delete
        }
        session.remove(object);
        return 1;
    }
}
