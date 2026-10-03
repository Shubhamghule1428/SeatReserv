package com.ssg.seatreserv.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ssg.seatreserv.entity.Seat;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID>{

	Optional<Seat> findByShowIdAndSeatNumber(UUID showId, String seatNo);
}
