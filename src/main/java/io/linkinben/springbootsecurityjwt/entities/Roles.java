package io.linkinben.springbootsecurityjwt.entities;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "roles", uniqueConstraints = { @UniqueConstraint(columnNames = "rId"),
		@UniqueConstraint(columnNames = "rName") })
public class Roles implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "rId")
	private String rId;

	@Column(name = "rName")
	private String rName;

	@ManyToMany(mappedBy = "roles")
	private Set<Users> users;

	private String getrId() {
		return rId;
	}

	private void setrId(String rId) {
		this.rId = rId;
	}

	private String getrName() {
		return rName;
	}

	private void setrName(String rName) {
		this.rName = rName;
	}

	private Set<Users> getUsers() {
		return users;
	}

	private void setUsers(Set<Users> users) {
		this.users = users;
	}

	public Roles() {

	}

	public Roles(String rId, String rName, Set<Users> users) {
		super();
		this.rId = rId;
		this.rName = rName;
		this.users = users;
	}
}
