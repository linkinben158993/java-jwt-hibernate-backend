package io.linkinben.springbootsecurityjwt.services;

import java.util.List;

public interface GenericService<T, K> {

	public List<T> findAll();

	public T findById(K id);

	public void add(T object);

	public int delete(K id);
	
	// Possible need of edit interface
}
