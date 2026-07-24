package io.linkinben.springbootsecurityjwt.dtos;

import java.sql.Date;

import jakarta.validation.constraints.NotBlank;

public class UserInfoDTO {

	// uId comes from the path/principal, never trusted from the body — no constraint.
	private String uId;
	@NotBlank
	private String fullName;
	// Optional profile fields on a partial (PATCH) update.
	private Integer age;
	private Date dob;

	public UserInfoDTO() {

	}

	public UserInfoDTO(@NotBlank String uId, @NotBlank String fullName, @NotBlank Integer age, @NotBlank Date dob) {
		super();
		this.uId = uId;
		this.fullName = fullName;
		this.age = age;
		this.dob = dob;
	}

	public String getuId() {
		return uId;
	}

	public void setuId(String uId) {
		this.uId = uId;
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

}
