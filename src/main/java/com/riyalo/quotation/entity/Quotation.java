package com.riyalo.quotation.entity;

import com.riyalo.quotation.enums.LayoutType;
import com.riyalo.quotation.enums.QuotationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "quotations")
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    private LocalDate date;

    private LocalDate validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LayoutType layoutType = LayoutType.CLASSIC;

    private String notes;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "item_order")
    private List<QuotationItem> items = new ArrayList<>();

    @Column(precision = 12, scale = 3)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 3)
    private BigDecimal taxAmount;

    @Column(precision = 12, scale = 3)
    private BigDecimal total;

    public void setSubtotal(BigDecimal subtotal) {
        if (subtotal != null) {
            this.subtotal = subtotal.setScale(3, RoundingMode.HALF_UP);
        } else {
            this.subtotal = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        if (taxAmount != null) {
            this.taxAmount = taxAmount.setScale(3, RoundingMode.HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
    }

    public void setTotal(BigDecimal total) {
        if (total != null) {
            this.total = total.setScale(3, RoundingMode.HALF_UP);
        } else {
            this.total = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
    }

    public void addItem(QuotationItem item) {
        items.add(item);
        item.setQuotation(this);
    }

    public void removeItem(QuotationItem item) {
        items.remove(item);
        item.setQuotation(null);
    }
}