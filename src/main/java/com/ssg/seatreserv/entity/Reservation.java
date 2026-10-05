package com.ssg.seatreserv.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.ssg.seatreserv.api.helper.ReservationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservations", indexes = { @Index(name = "idx_res_user", columnList = "user_id"),
		@Index(name = "idx_res_user_show", columnList = "user_id,show_id"),
		@Index(name = "idx_res_created_at", columnList = "created_at") })
public class Reservation {
	@Id
	@UuidGenerator
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", columnDefinition = "CHAR(36)")
	private UUID id;
	@Column(name = "show_id")
	private String showId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "seat_id")
	private UUID seatId;
	@Column(name = "user_id")
	private String userId;

//	@Enumerated(EnumType.ORDINAL)
	@Column(name = "status")
	private ReservationStatus status;
	@Column(name = "created_at")
	private Instant createdAt = Instant.now();

	public Reservation() {
	}

	public Reservation(String showId, UUID seatId, String userId, ReservationStatus status) {
		this.showId = showId;
		this.seatId = seatId;
		this.userId = userId;
		this.status = status;
	}

	public UUID getId() {
		return id;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public void setStatus(ReservationStatus status) {
		this.status = status;
	}

	public UUID getSeatId() {
		return seatId;
	}

	public String getShowId() {
		return showId;
	}

	public String getUserId() {
		return userId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setShowId(String showId) {
		this.showId = showId;
	}

}