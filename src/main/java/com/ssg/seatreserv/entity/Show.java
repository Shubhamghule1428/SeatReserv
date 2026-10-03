package com.ssg.seatreserv.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="shows")
public class Show {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private UUID showId;
	
	@Column(name = "name")
	private String name;
	
	@Column(name = "price")
	private long price_paise;
	
	@Column(name = "total_seat")
	private long totalSeat;

	@Column(name = "created_at")
	Instant createdAt = Instant.now();
}
