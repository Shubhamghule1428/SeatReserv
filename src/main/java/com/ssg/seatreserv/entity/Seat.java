package com.ssg.seatreserv.entity;

import java.util.UUID;

import com.ssg.seatreserv.api.helper.SeatStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "seats")
public class Seat {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "seat_id")
	private UUID seatId;
	
	@Column(name = "show_id")
	private UUID showId;
	
	@Column(name = "seat_no")
	private String seatNo;
	
	@Column(name = "status")
	private SeatStatus status;
	
	@Version
	@Column(name = "version")
	private long version;
	
	public UUID getSeatId() {
		return seatId;
	}

	public void setSeatId(UUID seatId) {
		this.seatId = seatId;
	}

	public UUID getShowId() {
		return showId;
	}

	public void setShowId(UUID showId) {
		this.showId = showId;
	}

	public String getSeatNo() {
		return seatNo;
	}

	public void setSeatNo(String seatNumber) {
		this.seatNo = seatNumber;
	}

	public SeatStatus getStatus() {
		return status;
	}

	public void setStatus(SeatStatus status) {
		this.status = status;
	}
	
	
}
