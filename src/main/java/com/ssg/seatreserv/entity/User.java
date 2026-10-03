package com.ssg.seatreserv.entity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "userid")
	private UUID userId;

	@Column(name = "name")
	private String name;

	@Column(name = "mob_no")
	private long mobNo;

	@Column(name = "token")
	private String token;
	
	public User(String name, long mobNo) {
		this.name = name;
		this.mobNo = mobNo;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getMobNo() {
		return mobNo;
	}

	public void setMobNo(long mobNo) {
		this.mobNo = mobNo;
	}

	public String getToken() {
		return token;
	}


	public void sha256() throws NoSuchAlgorithmException {
		String text = this.name + this.mobNo;

		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		byte[] encodedHash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

		String token = HexFormat.of().formatHex(encodedHash);
		this.token = token;
	}

}
