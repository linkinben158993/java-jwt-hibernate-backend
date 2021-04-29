package io.linkinben.springbootsecurityjwt.entities;

import java.sql.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "users")
@Table(name = "users")
@DynamicUpdate
public class Users extends GenericEntities<String> {

	@Id
	@Column(updatable = false, nullable = false)
	private String uId;

	@NotBlank
	@Email
	@Column(length = 120, unique = true)
	private String email;

	@NotBlank
	@Column(name = "full_name", length = 100)
	private String fullName;

	@Column(name = "age")
	private Integer age;

	@Column(name = "dob")
	private Date dob;

	@NotBlank
	@Column(length = 100)
	private String password;

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	// Many to many relationship joined table
	@JoinTable(name = "owned_roles", joinColumns = {
			@JoinColumn(name = "uId", referencedColumnName = "uId") }, inverseJoinColumns = {
					@JoinColumn(name = "rId", referencedColumnName = "rId") })
	// Infinite loop of many to many relationship
	@JsonIgnoreProperties("users")
	@NotEmpty
	private Set<Roles> roles;

	public String getuId() {
		return uId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Set<Roles> getRoles() {
		return roles;
	}

	public void setRoles(Set<Roles> roles) {
		this.roles = roles;
	}

	public void setuId(String uId) {
		this.uId = uId;
	}

	public Users() {

	}

	public Users(String uId, @NotBlank @Email String email, @NotBlank String fullName, @NotBlank String password) {
		this.uId = uId;
		this.email = email;
		this.fullName = fullName;
		this.password = password;
	}
}
