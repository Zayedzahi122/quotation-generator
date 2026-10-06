package com.riyalo.quotation.repository;

import com.riyalo.quotation.entity.CompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {

    Optional<CompanyProfile> findFirstByOrderByIdAsc();
}
