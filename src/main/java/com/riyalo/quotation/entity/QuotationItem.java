package com.riyalo.quotation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@Entity
@Table(name = "quotation_items")
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal unitPrice;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal discount;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal lineTotal;

    /** VAT rate for this item as a fraction, e.g. 0.0500 for 5%. Null means no VAT on this item. */
    @Column(precision = 6, scale = 4)
    private BigDecimal vatRate;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal taxAmount;

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice != null) {
            this.unitPrice = unitPrice.setScale(3, RoundingMode.HALF_UP);
        } else {
            this.unitPrice = null;
        }
    }

    public void setDiscount(BigDecimal discount) {
        if (discount != null) {
            this.discount = discount.setScale(3, RoundingMode.HALF_UP);
        } else {
            this.discount = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
    }

    public void setLineTotal(BigDecimal lineTotal) {
        if (lineTotal != null) {
            this.lineTotal = lineTotal.setScale(3, RoundingMode.HALF_UP);
        } else {
            this.lineTotal = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
    }

    public void setVatRate(BigDecimal vatRate) {
        if (vatRate != null && vatRate.compareTo(BigDecimal.ZERO) >= 0) {
            this.vatRate = vatRate.setScale(4, RoundingMode.HALF_UP);
        } else {
            this.vatRate = null;
        }
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        if (taxAmount != null) {
            this.taxAmount = taxAmount.setScale(3, RoundingMode.HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
    }

    /** VAT rate of this item in percent, e.g. 5.00. Returns 0.00 when no VAT is set. */
    public BigDecimal getVatPercent() {
        if (vatRate == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return vatRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Binds a VAT rate expressed in percent (e.g. 5) and stores it as a fraction. */
    public void setVatPercent(BigDecimal vatPercent) {
        if (vatPercent == null || vatPercent.compareTo(BigDecimal.ZERO) <= 0) {
            this.vatRate = null;
        } else {
            this.vatRate = vatPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
    }

    /** Line total including this item's VAT. */
    public BigDecimal getTotalWithTax() {
        BigDecimal line = lineTotal == null ? BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP) : lineTotal;
        BigDecimal tax = taxAmount == null ? BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP) : taxAmount;
        return line.add(tax).setScale(3, RoundingMode.HALF_UP);
    }

    public String getItemName() {
        return product == null ? null : product.getName();
    }
}