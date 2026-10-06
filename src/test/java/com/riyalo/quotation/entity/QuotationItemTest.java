package com.riyalo.quotation.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class QuotationItemTest {

    @Test
    void setVatPercent_storesFraction() {
        QuotationItem item = new QuotationItem();
        item.setVatPercent(new BigDecimal("5"));
        assertThat(item.getVatRate()).isEqualByComparingTo(new BigDecimal("0.0500"));
        assertThat(item.getVatPercent()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void setVatPercent_nullOrZeroClearsRate() {
        QuotationItem item = new QuotationItem();
        item.setVatPercent(BigDecimal.ZERO);
        assertThat(item.getVatRate()).isNull();

        item.setVatPercent(new BigDecimal("5"));
        item.setVatPercent(null);
        assertThat(item.getVatRate()).isNull();
        assertThat(item.getVatPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getVatPercent_handlesFractionRoundTrip() {
        QuotationItem item = new QuotationItem();
        item.setVatRate(new BigDecimal("0.0500"));
        assertThat(item.getVatPercent()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void getTotalWithTax_addsLineAndTax() {
        QuotationItem item = new QuotationItem();
        item.setLineTotal(new BigDecimal("100.000"));
        item.setTaxAmount(new BigDecimal("5.000"));
        assertThat(item.getTotalWithTax()).isEqualByComparingTo(new BigDecimal("105.000"));
    }

    @Test
    void getItemName_returnsNullWithoutProduct() {
        QuotationItem item = new QuotationItem();
        assertThat(item.getItemName()).isNull();

        Product product = new Product();
        product.setName("Desk");
        item.setProduct(product);
        assertThat(item.getItemName()).isEqualTo("Desk");
    }
}