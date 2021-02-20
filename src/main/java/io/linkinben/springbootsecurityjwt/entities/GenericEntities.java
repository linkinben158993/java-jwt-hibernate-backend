package io.linkinben.springbootsecurityjwt.entities;

public class GenericEntities<K> {
	private K id;

	public GenericEntities() {

	}

	public K getKey() {
		return id;
	}

	public void setId(K id) {
		this.id = id;
	}
}
