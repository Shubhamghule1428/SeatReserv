package com.ssg.seatreserv.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {
	@Id
	@Column(name = "key")
	private String key;
	@Column(name = "user_id")
	private String userId;
	@Column(name = "response_code")
	private int responseCode;
	@Column(name = "response_body", columnDefinition = "TEXT")
	private String responseBody;
	@Column(name = "created_at")
	private Instant createdAt = Instant.now();

	public IdempotencyRecord() {
	}

	public IdempotencyRecord(String key, String userId, int responseCode, String responseBody) {
		this.key = key;
		this.userId = userId;
		this.responseCode = responseCode;
		this.responseBody = responseBody;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getResponseBody() {
		return responseBody;
	}
}