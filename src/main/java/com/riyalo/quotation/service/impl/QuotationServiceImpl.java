package com.riyalo.quotation.service.impl;

import com.riyalo.quotation.entity.Customer;
import com.riyalo.quotation.entity.Product;
import com.riyalo.quotation.entity.Quotation;
import com.riyalo.quotation.entity.QuotationItem;
import com.riyalo.quotation.enums.LayoutType;
import com.riyalo.quotation.enums.QuotationStatus;
import com.riyalo.quotation.repository.CustomerRepository;
import com.riyalo.quotation.repository.ProductRepository;
import com.riyalo.quotation.repository.QuotationRepository;
import com.riyalo.quotation.service.QuotationNumberingService;
import com.riyalo.quotation.service.QuotationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuotationServiceImpl implements QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationNumberingService quotationNumberingService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final BigDecimal defaultVatRate;

    public QuotationServiceImpl(QuotationRepository quotationRepository,
                                QuotationNumberingService quotationNumberingService,
                                CustomerRepository customerRepository,
                                ProductRepository productRepository,
                                @Value("${quotation.vat.rate:0.05}") BigDecimal defaultVatRate) {
        this.quotationRepository = quotationRepository;
        this.quotationNumberingService = quotationNumberingService;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.defaultVatRate = defaultVatRate;
    }

    @Transactional
    @Override
    public Quotation createDraft(Quotation quotation) {
        if (quotation.getNumber() == null || quotation.getNumber().isBlank()) {
            int year = quotation.getDate() == null ? LocalDate.now().getYear() : quotation.getDate().getYear();
            quotation.setNumber(quotationNumberingService.generateNextNumber(year));
        }
        if (quotation.getStatus() == null) {
            quotation.setStatus(QuotationStatus.DRAFT);
        }
        if (quotation.getLayoutType() == null) {
            quotation.setLayoutType(LayoutType.CLASSIC);
        }
        if (quotation.getTaxAmount() == null) {
            quotation.setTaxAmount(BigDecimal.ZERO);
        }
        if (quotation.getSubtotal() == null) {
            quotation.setSubtotal(BigDecimal.ZERO);
        }
        if (quotation.getTotal() == null) {
            quotation.setTotal(BigDecimal.ZERO);
        }
        if (quotation.getItems() != null) {
            for (QuotationItem item : quotation.getItems()) {
                item.setQuotation(quotation);
            }
        }
        return quotationRepository.save(quotation);
    }

    @Transactional(readOnly = true)
    @Override
    public Quotation findById(Long id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found: " + id));
    }

    @Transactional(readOnly = true)
    @Override
    public Quotation findByNumber(String number) {
        return quotationRepository.findByNumber(number)
                .orElseThrow(() -> new EntityNotFoundException("Quotation not found: " + number));
    }

    @Transactional(readOnly = true)
    @Override
    public List<Quotation> findAll() {
        return quotationRepository.findAll(Sort.by(Sort.Direction.DESC, "date"));
    }

    @Transactional
    @Override
    public Quotation updateQuotation(Quotation quotation) {
        if (quotation.getId() == null) {
            throw new IllegalArgumentException("Quotation id is required for update");
        }
        Quotation existing = findById(quotation.getId());
        existing.setDate(quotation.getDate());
        existing.setValidUntil(quotation.getValidUntil());
        existing.setCustomer(quotation.getCustomer());
        existing.setStatus(quotation.getStatus());
        existing.setLayoutType(quotation.getLayoutType() == null ? existing.getLayoutType() : quotation.getLayoutType());
        existing.setNotes(quotation.getNotes());
        return quotationRepository.save(existing);
    }

    @Transactional
    @Override
    public Quotation updateLayout(Long id, LayoutType layoutType) {
        if (layoutType == null) {
            throw new IllegalArgumentException("Layout type is required");
        }
        Quotation quotation = findById(id);
        quotation.setLayoutType(layoutType);
        return quotationRepository.save(quotation);
    }

    @Transactional
    @Override
    public Quotation recalculateTotals(Long id) {
        Quotation quotation = findById(id);
        return recalculateTotals(quotation);
    }

    private Quotation recalculateTotals(Quotation quotation) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        if (quotation.getItems() != null) {
            for (QuotationItem item : quotation.getItems()) {
                subtotal = subtotal.add(computeItemTotals(item));
                taxTotal = taxTotal.add(item.getTaxAmount());
            }
        }
        subtotal = subtotal.setScale(3, RoundingMode.HALF_UP);
        taxTotal = taxTotal.setScale(3, RoundingMode.HALF_UP);
        quotation.setSubtotal(subtotal);
        quotation.setTaxAmount(taxTotal);
        quotation.setTotal(subtotal.add(taxTotal).setScale(3, RoundingMode.HALF_UP));
        return quotationRepository.save(quotation);
    }

    /** Recomputes and stores line total and VAT tax for a single item, returning the line total. */
    private BigDecimal computeItemTotals(QuotationItem item) {
        BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : BigDecimal.valueOf(item.getQuantity());
        BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
        BigDecimal discount = item.getDiscount() == null ? BigDecimal.ZERO : item.getDiscount();
        BigDecimal line = qty.multiply(unitPrice).subtract(discount);
        if (line.compareTo(BigDecimal.ZERO) < 0) {
            line = BigDecimal.ZERO;
        }
        line = line.setScale(3, RoundingMode.HALF_UP);
        item.setLineTotal(line);

        BigDecimal vatRate = item.getVatRate();
        BigDecimal tax = vatRate == null
                ? BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
                : line.multiply(vatRate).setScale(3, RoundingMode.HALF_UP);
        item.setTaxAmount(tax);
        return line;
    }

    @Transactional
    @Override
    public Quotation setValidUntil(Long id, LocalDate validUntil) {
        Quotation quotation = findById(id);
        quotation.setValidUntil(validUntil);
        return quotationRepository.save(quotation);
    }

    @Transactional
    @Override
    public Quotation applyVat(Long id, boolean applyVat, BigDecimal vatRate) {
        if (applyVat && (vatRate == null || vatRate.compareTo(BigDecimal.ZERO) < 0)) {
            throw new IllegalArgumentException("VAT rate must be non-negative");
        }
        Quotation quotation = findById(id);
        BigDecimal rate = applyVat ? vatRate.setScale(4, RoundingMode.HALF_UP) : null;
        if (quotation.getItems() != null) {
            for (QuotationItem item : quotation.getItems()) {
                item.setVatRate(rate);
            }
        }
        return recalculateTotals(quotation);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        if (!quotationRepository.existsById(id)) {
            throw new EntityNotFoundException("Quotation not found: " + id);
        }
        quotationRepository.deleteById(id);
    }

    @Transactional
    @Override
    public Quotation createFromForm(Quotation form) {
        form.setId(null);
        applyForm(form, form);
        Quotation saved = createDraft(form);
        return recalculateTotals(saved);
    }

    @Transactional
    @Override
    public Quotation updateFromForm(Quotation form) {
        if (form.getId() == null) {
            throw new IllegalArgumentException("Quotation id is required for update");
        }
        Quotation existing = findById(form.getId());
        existing.setDate(form.getDate());
        existing.setValidUntil(form.getValidUntil());
        existing.setStatus(form.getStatus());
        if (form.getLayoutType() != null) {
            existing.setLayoutType(form.getLayoutType());
        }
        existing.setNotes(form.getNotes());
        applyForm(existing, form);
        Quotation saved = quotationRepository.save(existing);
        return recalculateTotals(saved);
    }

    @Transactional
    @Override
    public Customer findOrCreateCustomer(Customer submitted) {
        if (submitted == null || submitted.getName() == null || submitted.getName().isBlank()) {
            return null;
        }
        String name = submitted.getName().trim();
        Customer existing = customerRepository.findFirstByNameIgnoreCase(name).orElse(null);
        boolean created = existing == null;
        Customer customer;
        if (created) {
            customer = new Customer();
            customer.setName(name);
            customer.setCreatedAt(LocalDateTime.now());
        } else {
            customer = existing;
        }
        if (applyCustomerDetails(customer, submitted) || created) {
            customer = customerRepository.save(customer);
        }
        return customer;
    }

    /** Copies address, phone and email from the submitted values onto the customer when they are filled in. */
    private boolean applyCustomerDetails(Customer customer, Customer submitted) {
        boolean changed = false;
        if (isPresent(submitted.getAddress())) {
            customer.setAddress(submitted.getAddress().trim());
            changed = true;
        }
        if (isPresent(submitted.getPhone())) {
            customer.setPhone(submitted.getPhone().trim());
            changed = true;
        }
        if (isPresent(submitted.getEmail())) {
            customer.setEmail(submitted.getEmail().trim());
            changed = true;
        }
        return changed;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    @Transactional
    @Override
    public Product findOrCreateProductByName(String name, BigDecimal unitPrice) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        return productRepository.findFirstByNameIgnoreCase(trimmed)
                .orElseGet(() -> {
                    Product product = new Product();
                    product.setName(trimmed);
                    product.setUnitPrice(unitPrice == null ? BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP) : unitPrice);
                    return productRepository.save(product);
                });
    }

    @Transactional
    @Override
    public Quotation applyVat(Long id, boolean applyVat) {
        return applyVat(id, applyVat, defaultVatRate);
    }

    @Override
    public BigDecimal getDefaultVatRate() {
        return defaultVatRate;
    }

    private void applyForm(Quotation quotation, Quotation form) {
        quotation.setCustomer(findOrCreateCustomer(form.getCustomer()));

        List<QuotationItem> submittedItems = form.getItems();
        List<QuotationItem> resolved = new ArrayList<>();
        if (submittedItems != null) {
            for (QuotationItem submitted : submittedItems) {
                if (submitted == null) {
                    continue;
                }
                String itemName = submitted.getProduct() == null ? null : submitted.getProduct().getName();
                if (itemName == null || itemName.isBlank()) {
                    continue;
                }
                BigDecimal unitPrice = submitted.getUnitPrice() == null
                        ? BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
                        : submitted.getUnitPrice();
                QuotationItem item = new QuotationItem();
                item.setProduct(findOrCreateProductByName(itemName, unitPrice));
                item.setQuantity(submitted.getQuantity() == null || submitted.getQuantity() < 1 ? 1 : submitted.getQuantity());
                item.setUnitPrice(unitPrice);
                item.setDiscount(submitted.getDiscount());
                item.setVatRate(normalizeVatRate(submitted.getVatRate()));
                item.setQuotation(quotation);
                computeItemTotals(item);
                resolved.add(item);
            }
        }
        if (quotation.getItems() == null) {
            quotation.setItems(new ArrayList<>());
        }
        quotation.getItems().clear();
        quotation.getItems().addAll(resolved);
    }

    private BigDecimal normalizeVatRate(BigDecimal vatRate) {
        if (vatRate == null || vatRate.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return vatRate.setScale(4, RoundingMode.HALF_UP);
    }
}