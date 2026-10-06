package com.riyalo.quotation.service;

import com.riyalo.quotation.entity.Customer;
import com.riyalo.quotation.entity.Product;
import com.riyalo.quotation.entity.Quotation;
import com.riyalo.quotation.enums.LayoutType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface QuotationService {
    Quotation createDraft(Quotation quotation);
    Quotation findById(Long id);
    Quotation findByNumber(String number);
    List<Quotation> findAll();
    Quotation updateQuotation(Quotation quotation);
    Quotation updateLayout(Long id, LayoutType layoutType);
    Quotation recalculateTotals(Long id);
    Quotation setValidUntil(Long id, LocalDate validUntil);
    Quotation applyVat(Long id, boolean applyVat, BigDecimal vatRate);
    void deleteById(Long id);

    /** Creates a quotation from submitted form data, resolving customer and product names. */
    Quotation createFromForm(Quotation form);

    /** Updates a quotation from submitted form data, resolving customer and product names. */
    Quotation updateFromForm(Quotation form);

    /** Returns the existing customer with the given name, or creates a new one, saving any contact details provided. */
    Customer findOrCreateCustomer(Customer submitted);

    /** Returns the existing product with the given name, or creates a new one. */
    Product findOrCreateProductByName(String name, BigDecimal unitPrice);

    /** Applies (or removes) the configured default VAT rate on every item. */
    Quotation applyVat(Long id, boolean applyVat);

    /** Configured default VAT rate as a fraction, e.g. 0.05 for 5%. */
    BigDecimal getDefaultVatRate();
}