package io.linkinben.springbootsecurityjwt.services.impl;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import io.linkinben.springbootsecurityjwt.entities.GenericEntities;
import io.linkinben.springbootsecurityjwt.repositories.GenericRepository;
import io.linkinben.springbootsecurityjwt.services.GenericService;

public abstract class GenericServiceImpl<T extends GenericEntities<String>, K extends Serializable> implements GenericService<T, K> {

	@Autowired
	protected GenericRepository<T, K> genericRepository;

	public List<T> findAll() {
		return genericRepository.findAll();
	}

	public T findById(K id) {
		return genericRepository.findById(id);
	}

	public void add(T object) {
		String id = UUID.randomUUID().toString();
		object.setId(id);
		genericRepository.insert(object);
	}

	public int delete(K id) {
		if (genericRepository.removeById(id) == 1) {
			return 1;
		}
		return 0;
	}
}
