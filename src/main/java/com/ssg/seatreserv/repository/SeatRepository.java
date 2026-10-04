package com.ssg.seatreserv.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ssg.seatreserv.entity.Seat;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

	Optional<Seat> findByShowIdAndSeatNumber(String showId, String seatNo);
	
	@Query("select s from Seat s where s.showId = :showId AND seatNumber IN (:seatNumbers)")
	List<Seat> findByShowIdAndSeatNumbers(@Param("showId") String showId, @Param("seatNumbers") List<String> seatNumbers);

	@Query("select s.status as status, count(s) as count from Seat s where s.showId = :showId group by s.status")
	Map<String, Long> countSeatsByStatus(@Param("showId") String showId);
	
	@Query("select s from Seat s where s.showId = :showId")
	List<Seat> findByShowId(@Param("showId") String showId);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
	    UPDATE Seat s 
	    SET s.status = 2, s.version = s.version + 1 
	    WHERE s.showId = :showId 
	      AND s.seatNumber IN (:seatNumbers) 
	      AND s.status = 0
	""")
	int reserveSeatsAtomically(@Param("showId") String showId, @Param("seatNumbers") List<String> seatNumbers);
}
 