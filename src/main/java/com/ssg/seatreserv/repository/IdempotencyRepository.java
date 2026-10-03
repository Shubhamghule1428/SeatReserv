package com.ssg.seatreserv.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssg.seatreserv.entity.IdempotencyRecord;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {

}
