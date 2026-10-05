package com.ssg.seatreserv.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.ssg.seatreserv.api.helper.SeatStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "seats", indexes = {
		@Index(name = "idx_seats_show_status", columnList = "show_id,status") }, uniqueConstraints = {
				@UniqueConstraint(name = "uq_show_seat", columnNames = { "show_id", "seat_no" }) })
public class Seat {

	@Id
	@UuidGenerator
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", columnDefinition = "CHAR(36)")
	private UUID seatId;

	@Column(name = "show_id")
	private String showId;

	@Column(name = "seat_no")
	private String seatNumber;

	@Column(name = "status")
	private SeatStatus status;

	@Version
	@Column(name = "version")
	private long version;

	public Seat(String showId, String seatNumber) {
		this.showId = showId;
		this.seatNumber = seatNumber;

	}

	public Seat() {
	}

	public UUID getSeatId() {
		return seatId;
	}

	public void setSeatId(UUID seatId) {
		this.seatId = seatId;
	}

	public String getShowId() {
		return showId;
	}

	public void setShowId(String showId) {
		this.showId = showId;
	}

	public SeatStatus getStatus() {
		return status;
	}

	public void setStatus(SeatStatus status) {
		this.status = status;
	}

	public String getSeatNumber() {
		return seatNumber;
	}

	public void setSeatNumber(String seatNumber) {
		this.seatNumber = seatNumber;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public static Seat createSeat(String showId, String seatNumber) {
		Seat seat = new Seat(showId, seatNumber);
		;
		seat.setStatus(SeatStatus.AVAILABLE);
		return seat;
	}

}
