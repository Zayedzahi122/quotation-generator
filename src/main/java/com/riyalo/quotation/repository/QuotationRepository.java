package com.riyalo.quotation.repository;

import com.riyalo.quotation.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    @Query("SELECT q FROM Quotation q WHERE q.number = :number")
    Optional<Quotation> findByNumber(@Param("number") String number);

    @Query("SELECT MAX(q.number) FROM Quotation q WHERE q.number LIKE CONCAT('RYL-', :year, '-%')")
    String findMaxNumberForYear(@Param("year") int year);
}