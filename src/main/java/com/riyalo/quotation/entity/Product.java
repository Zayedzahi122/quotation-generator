package com.riyalo.quotation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal unitPrice;

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice != null) {
            this.unitPrice = unitPrice.setScale(3, RoundingMode.HALF_UP);
        } else {
            this.unitPrice = null;
        }
    }
}