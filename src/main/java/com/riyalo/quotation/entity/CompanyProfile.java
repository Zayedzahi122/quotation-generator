package com.riyalo.quotation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Singleton row holding the details of our own company, shown in the "From" section
 * of every quotation, including the uploaded logo file name.
 */
@Getter
@Setter
@Entity
@Table(name = "company_profile")
public class CompanyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName = "Your Company Name";

    private String address;

    private String phone;

    private String email;

    private String logoFileName;
}
