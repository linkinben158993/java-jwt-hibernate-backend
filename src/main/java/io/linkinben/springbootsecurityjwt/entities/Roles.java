package io.linkinben.springbootsecurityjwt.entities;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;

@Entity(name = "roles")
@Table(name = "roles", uniqueConstraints = { @UniqueConstraint(columnNames = "rId"),
		@UniqueConstraint(columnNames = "rName") })
public class Roles extends GenericEntities<String> {

	@Id
	@Column(name = "rId")
	private String rId;

	@NotBlank
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
