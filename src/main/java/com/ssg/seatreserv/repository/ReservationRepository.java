package com.ssg.seatreserv.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssg.seatreserv.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

	@Query("""
	        SELECT COUNT(r) FROM Reservation r 
	        WHERE r.userId = :userId 
	          AND r.showId = :showId 
	          AND r.status = 'CONFIRMED'
	    """)
	    long countActiveBookings(@Param("userId") String userId, @Param("showId") String showId);
	
	Optional<Reservation> findByIdAndShowIdAndStatus(String id, String showId, String status);
}
