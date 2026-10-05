package com.ssg.seatreserv.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "shows", indexes = { @Index(name = "idx_shows_created_at", columnList = "created_at") })
public class Show {

	@Id
	@Column(name = "id")
	private String showId;

	@Column(name = "name")
	private String name;

	@Column(name = "price")
	private long pricePaise;

	@Column(name = "total_seats")
	private long totalSeat;

	@Column(name = "user_seat_limit")
	private int userSeatLimit;

	@Column(name = "created_at")
	Instant createdAt = Instant.now();

	public Show() {
	}

	public Show(String name, long price_paise, long totalSeat, int userSeatLImit) {
		this.name = name;
		this.pricePaise = price_paise;
		this.totalSeat = totalSeat;
		this.userSeatLimit = userSeatLImit;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getPricePaise() {
		return pricePaise;
	}

	public void setPricePaise(long price_paise) {
		this.pricePaise = price_paise;
	}

	public long getTotalSeat() {
		return totalSeat;
	}

	public void setTotalSeat(long totalSeat) {
		this.totalSeat = totalSeat;
	}

	public int getUserSeatLimit() {
		return this.userSeatLimit;
	}

	public void setUserSeatLimit(int userSeatLimit) {
		this.userSeatLimit = userSeatLimit;
	}

	public String getShowId() {
		return showId;
	}

	public void setShowId(String showId) {
		this.showId = showId;
	}

	@PrePersist
	public void generateId() {
		if (this.showId == null) {
			this.showId = name + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		}
	}

}
