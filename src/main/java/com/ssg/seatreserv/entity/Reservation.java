package com.ssg.seatreserv.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservations")
public class Reservation {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private UUID id;
	@Column(name = "show_id")
	private UUID showId;
	@Column(name = "seat_id")
	private UUID seatId;
	@Column(name = "user_id")
	private String userId;
	@Column(name = "status")
	private String status;
	@Column(name = "created_at")
	private Instant createdAt = Instant.now();

	public Reservation() {
	}

	public Reservation(UUID showId, UUID seatId, String userId, String status) {
		this.showId = showId;
		this.seatId = seatId;
		this.userId = userId;
		this.status = status;
	}

	public UUID getId() {
		return id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public UUID getSeatId() {
		return seatId;
	}
}