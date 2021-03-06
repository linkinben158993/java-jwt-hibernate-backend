package io.linkinben.springbootsecurityjwt.entities;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity(name = "roles")
@Table(name = "roles", uniqueConstraints = { @UniqueConstraint(columnNames = "rId"),
		@UniqueConstraint(columnNames = "rName") })
public class Roles extends GenericEntities<String> {

	@Id
	@Column(name = "rId")
	private String rId;

	@Column(name = "rName", unique = true)
	private String rName;

	@ManyToMany(mappedBy = "roles", fetch = FetchType.EAGER)
	private Set<Users> users;

	public String getrId() {
		return rId;
	}

	public void setrId(String rId) {
		this.rId = rId;
	}

	public String getrName() {
		return rName;
	}

	public void setrName(String rName) {
		this.rName = rName;
	}

	public Set<Users> getUsers() {
		return users;
	}

	public void setUsers(Set<Users> users) {
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
