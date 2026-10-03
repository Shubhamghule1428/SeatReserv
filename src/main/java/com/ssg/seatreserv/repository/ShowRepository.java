package com.ssg.seatreserv.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssg.seatreserv.entity.Seat;
import com.ssg.seatreserv.entity.Show;

public interface ShowRepository extends JpaRepository<Show, UUID>{

}
