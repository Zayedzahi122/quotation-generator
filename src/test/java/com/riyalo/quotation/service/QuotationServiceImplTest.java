package com.riyalo.quotation.service;

import com.riyalo.quotation.entity.Customer;
import com.riyalo.quotation.entity.Product;
import com.riyalo.quotation.entity.Quotation;
import com.riyalo.quotation.entity.QuotationItem;
import com.riyalo.quotation.enums.LayoutType;
import com.riyalo.quotation.enums.QuotationStatus;
import com.riyalo.quotation.repository.CustomerRepository;
import com.riyalo.quotation.repository.ProductRepository;
import com.riyalo.quotation.repository.QuotationRepository;
import com.riyalo.quotation.service.impl.QuotationNumberingServiceImpl;
import com.riyalo.quotation.service.impl.QuotationServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotationServiceImplTest {

    private static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("0.05");

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationNumberingService quotationNumberingService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    private QuotationServiceImpl quotationService;

    private Quotation quotation;

    @BeforeEach
    void setUp() {
        quotationService = new QuotationServiceImpl(
                quotationRepository, quotationNumberingService, customerRepository, productRepository, DEFAULT_VAT_RATE);
        quotation = new Quotation();
        quotation.setId(1L);
        quotation.setNumber("RYL-2026-001");
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.setLayoutType(LayoutType.CLASSIC);
        quotation.setSubtotal(BigDecimal.ZERO);
        quotation.setTaxAmount(BigDecimal.ZERO);
        quotation.setTotal(BigDecimal.ZERO);
    }

    private QuotationItem item(int qty, String unitPrice, String discount, String vatPercent) {
        QuotationItem item = new QuotationItem();
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setDiscount(new BigDecimal(discount));
        if (vatPercent != null) {
            item.setVatRate(new BigDecimal(vatPercent).divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
        }
        return item;
    }

    @Test
    void createDraft_setsDefaultsWhenNull() {
        Quotation input = new Quotation();
        input.setDate(LocalDate.of(2026, 1, 1));
        when(quotationNumberingService.generateNextNumber(2026)).thenReturn("RYL-2026-001");
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.createDraft(input);

        assertThat(result.getNumber()).isEqualTo("RYL-2026-001");
        assertThat(result.getStatus()).isEqualTo(QuotationStatus.DRAFT);
        assertThat(result.getLayoutType()).isEqualTo(LayoutType.CLASSIC);
        assertThat(result.getTaxAmount()).isEqualTo(BigDecimal.ZERO.setScale(3));
        assertThat(result.getSubtotal()).isEqualTo(BigDecimal.ZERO.setScale(3));
        assertThat(result.getTotal()).isEqualTo(BigDecimal.ZERO.setScale(3));
        verify(quotationNumberingService).generateNextNumber(2026);
        verify(quotationRepository).save(any(Quotation.class));
    }

    @Test
    void createDraft_preservesExistingNumberAndStatus() {
        Quotation input = new Quotation();
        input.setNumber("RYL-2026-005");
        input.setStatus(QuotationStatus.SENT);
        input.setLayoutType(LayoutType.MODERN);
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.createDraft(input);

        assertThat(result.getNumber()).isEqualTo("RYL-2026-005");
        assertThat(result.getStatus()).isEqualTo(QuotationStatus.SENT);
        assertThat(result.getLayoutType()).isEqualTo(LayoutType.MODERN);
        verify(quotationNumberingService, never()).generateNextNumber(anyInt());
    }

    @Test
    void findById_found() {
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        Quotation result = quotationService.findById(1L);
        assertThat(result).isEqualTo(quotation);
    }

    @Test
    void findById_notFound() {
        when(quotationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> quotationService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Quotation not found");
    }

    @Test
    void findByNumber_found() {
        when(quotationRepository.findByNumber("RYL-2026-001")).thenReturn(Optional.of(quotation));
        Quotation result = quotationService.findByNumber("RYL-2026-001");
        assertThat(result).isEqualTo(quotation);
    }

    @Test
    void findByNumber_notFound() {
        when(quotationRepository.findByNumber("RYL-2026-999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> quotationService.findByNumber("RYL-2026-999"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Quotation not found");
    }

    @Test
    void updateQuotation_updatesFields() {
        Quotation update = new Quotation();
        update.setId(1L);
        update.setDate(LocalDate.of(2026, 5, 1));
        update.setValidUntil(LocalDate.of(2026, 6, 1));
        Customer customer = new Customer();
        customer.setId(5L);
        update.setCustomer(customer);
        update.setStatus(QuotationStatus.SENT);
        update.setLayoutType(LayoutType.MODERN);
        update.setNotes("Updated");

        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.updateQuotation(update);

        assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(result.getValidUntil()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getStatus()).isEqualTo(QuotationStatus.SENT);
        assertThat(result.getLayoutType()).isEqualTo(LayoutType.MODERN);
        assertThat(result.getNotes()).isEqualTo("Updated");
        verify(quotationRepository).save(any(Quotation.class));
    }

    @Test
    void updateQuotation_requiresId() {
        Quotation update = new Quotation();
        assertThatThrownBy(() -> quotationService.updateQuotation(update))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateLayout_updatesLayout() {
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.updateLayout(1L, LayoutType.COMPACT);

        assertThat(result.getLayoutType()).isEqualTo(LayoutType.COMPACT);
    }

    @Test
    void updateLayout_requiresLayoutType() {
        assertThatThrownBy(() -> quotationService.updateLayout(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recalculateTotals_calculatesLineTotalsAndSubtotal() {
        Quotation quote = new Quotation();
        quote.setId(1L);
        quote.setTaxAmount(BigDecimal.ZERO);

        QuotationItem item1 = item(2, "50.000", "0.000", null);
        QuotationItem item2 = item(1, "30.000", "5.000", null);

        List<QuotationItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        quote.setItems(items);

        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.recalculateTotals(1L);

        assertThat(item1.getLineTotal()).isEqualTo(BigDecimal.valueOf(100.000).setScale(3));
        assertThat(item2.getLineTotal()).isEqualTo(BigDecimal.valueOf(25.000).setScale(3));
        assertThat(result.getSubtotal()).isEqualTo(BigDecimal.valueOf(125.000).setScale(3));
        assertThat(result.getTotal()).isEqualTo(BigDecimal.valueOf(125.000).setScale(3));
    }

    @Test
    void recalculateTotals_clampsNegativeLineTotalToZero() {
        Quotation quote = new Quotation();
        quote.setId(1L);
        quote.setTaxAmount(BigDecimal.ZERO);

        QuotationItem item = item(1, "10.000", "20.000", null);
        quote.setItems(List.of(item));

        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.recalculateTotals(1L);

        assertThat(item.getLineTotal()).isEqualTo(BigDecimal.ZERO.setScale(3));
        assertThat(result.getSubtotal()).isEqualTo(BigDecimal.ZERO.setScale(3));
    }

    @Test
    void recalculateTotals_computesTaxPerItemAndTotals() {
        Quotation quote = new Quotation();
        quote.setId(1L);
        quote.setTaxAmount(BigDecimal.ZERO);

        QuotationItem taxed = item(2, "100.000", "0.000", "5");
        QuotationItem untaxed = item(1, "50.000", "0.000", null);
        quote.setItems(new ArrayList<>(List.of(taxed, untaxed)));

        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.recalculateTotals(1L);

        assertThat(taxed.getLineTotal()).isEqualTo(new BigDecimal("200.000"));
        assertThat(taxed.getTaxAmount()).isEqualTo(new BigDecimal("10.000"));
        assertThat(untaxed.getTaxAmount()).isEqualTo(BigDecimal.ZERO.setScale(3));
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("250.000"));
        assertThat(result.getTaxAmount()).isEqualTo(new BigDecimal("10.000"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("260.000"));
    }

    @Test
    void setValidUntil_setsValidUntil() {
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));
        LocalDate date = LocalDate.of(2026, 12, 31);

        Quotation result = quotationService.setValidUntil(1L, date);

        assertThat(result.getValidUntil()).isEqualTo(date);
    }

    @Test
    void applyVat_removesVatFromEveryItem() {
        Quotation quote = new Quotation();
        quote.setId(1L);
        quote.setSubtotal(BigDecimal.valueOf(100.000));
        quote.setTaxAmount(BigDecimal.valueOf(5.000));
        QuotationItem item = item(1, "100.000", "0.000", "5");
        quote.setItems(new ArrayList<>(List.of(item)));

        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.applyVat(1L, false, BigDecimal.valueOf(0.05));

        assertThat(item.getVatRate()).isNull();
        assertThat(item.getTaxAmount()).isEqualTo(BigDecimal.ZERO.setScale(3));
        assertThat(result.getTaxAmount()).isEqualTo(BigDecimal.ZERO.setScale(3));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("100.000"));
    }

    @Test
    void applyVat_withVatCalculatesTaxPerItem() {
        Quotation quote = new Quotation();
        quote.setId(1L);
        quote.setSubtotal(BigDecimal.valueOf(100.000));
        quote.setTaxAmount(BigDecimal.ZERO);
        QuotationItem item = item(1, "100.000", "0.000", null);
        quote.setItems(new ArrayList<>(List.of(item)));

        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.applyVat(1L, true, BigDecimal.valueOf(0.05));

        assertThat(item.getVatRate()).isEqualByComparingTo(new BigDecimal("0.0500"));
        assertThat(item.getTaxAmount()).isEqualTo(new BigDecimal("5.000"));
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("100.000"));
        assertThat(result.getTaxAmount()).isEqualTo(new BigDecimal("5.000"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("105.000"));
    }

    @Test
    void applyVat_usesConfiguredDefaultRate() {
        Quotation quote = new Quotation();
        quote.setId(1L);
        quote.setSubtotal(BigDecimal.valueOf(200.000));
        quote.setTaxAmount(BigDecimal.ZERO);
        QuotationItem item = item(1, "200.000", "0.000", null);
        quote.setItems(new ArrayList<>(List.of(item)));

        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.applyVat(1L, true);

        assertThat(result.getTaxAmount()).isEqualTo(new BigDecimal("10.000"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("210.000"));
    }

    @Test
    void applyVat_rejectsNegativeRate() {
        assertThatThrownBy(() -> quotationService.applyVat(1L, true, BigDecimal.valueOf(-0.01)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getDefaultVatRate_returnsConfiguredRate() {
        assertThat(quotationService.getDefaultVatRate()).isEqualByComparingTo(DEFAULT_VAT_RATE);
    }

    @Test
    void findOrCreateCustomer_returnsExistingTrimmed() {
        Customer existing = new Customer();
        existing.setId(7L);
        existing.setName("John Doe");
        when(customerRepository.findFirstByNameIgnoreCase("John Doe")).thenReturn(Optional.of(existing));

        Customer submitted = new Customer();
        submitted.setName("  John Doe ");

        Customer result = quotationService.findOrCreateCustomer(submitted);

        assertThat(result).isEqualTo(existing);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void findOrCreateCustomer_createsNewWhenMissing() {
        when(customerRepository.findFirstByNameIgnoreCase("Jane")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        Customer submitted = new Customer();
        submitted.setName("Jane");

        Customer result = quotationService.findOrCreateCustomer(submitted);

        assertThat(result.getName()).isEqualTo("Jane");
        assertThat(result.getCreatedAt()).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void findOrCreateCustomer_blankNameReturnsNull() {
        assertThat(quotationService.findOrCreateCustomer(new Customer())).isNull();
        assertThat(quotationService.findOrCreateCustomer(null)).isNull();
        verify(customerRepository, never()).findFirstByNameIgnoreCase(any());
    }

    @Test
    void findOrCreateCustomer_fillsContactDetailsOnExisting() {
        Customer existing = new Customer();
        existing.setId(7L);
        existing.setName("John Doe");
        existing.setPhone("+968 9999");
        when(customerRepository.findFirstByNameIgnoreCase("John Doe")).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        Customer submitted = new Customer();
        submitted.setName("John Doe");
        submitted.setAddress("Muscat, Oman");
        submitted.setEmail("john@example.com");

        Customer result = quotationService.findOrCreateCustomer(submitted);

        assertThat(result.getAddress()).isEqualTo("Muscat, Oman");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getPhone()).isEqualTo("+968 9999"); // blank submitted value keeps existing
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void findOrCreateProductByName_returnsExisting() {
        Product existing = new Product();
        existing.setId(3L);
        existing.setName("Laptop");
        when(productRepository.findFirstByNameIgnoreCase("Laptop")).thenReturn(Optional.of(existing));

        Product result = quotationService.findOrCreateProductByName("Laptop", new BigDecimal("900.000"));

        assertThat(result).isEqualTo(existing);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void findOrCreateProductByName_createsNewWithSuppliedPrice() {
        when(productRepository.findFirstByNameIgnoreCase("Mouse")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        Product result = quotationService.findOrCreateProductByName("Mouse", new BigDecimal("2.500"));

        assertThat(result.getName()).isEqualTo("Mouse");
        assertThat(result.getUnitPrice()).isEqualTo(new BigDecimal("2.500"));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void findOrCreateProductByName_blankReturnsNull() {
        assertThat(quotationService.findOrCreateProductByName(null, null)).isNull();
        verify(productRepository, never()).findFirstByNameIgnoreCase(any());
    }

    @Test
    void createFromForm_resolvesCustomerAndItemsAndRecalculates() {
        Customer customer = new Customer();
        customer.setId(9L);
        customer.setName("Acme");
        Product product = new Product();
        product.setId(4L);
        product.setName("Desk");

        Quotation form = new Quotation();
        form.setDate(LocalDate.of(2026, 6, 1));
        Customer submittedCustomer = new Customer();
        submittedCustomer.setName("Acme");
        form.setCustomer(submittedCustomer);

        QuotationItem submittedItem = item(3, "100.000", "0.000", "5");
        Product submittedProduct = new Product();
        submittedProduct.setName("Desk");
        submittedItem.setProduct(submittedProduct);
        form.setItems(new ArrayList<>(List.of(submittedItem)));

        when(customerRepository.findFirstByNameIgnoreCase("Acme")).thenReturn(Optional.of(customer));
        when(productRepository.findFirstByNameIgnoreCase("Desk")).thenReturn(Optional.of(product));
        when(quotationNumberingService.generateNextNumber(2026)).thenReturn("RYL-2026-001");
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.createFromForm(form);

        assertThat(result.getNumber()).isEqualTo("RYL-2026-001");
        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getItems()).hasSize(1);
        QuotationItem savedItem = result.getItems().get(0);
        assertThat(savedItem.getProduct()).isEqualTo(product);
        assertThat(savedItem.getQuotation()).isEqualTo(result);
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("300.000"));
        assertThat(result.getTaxAmount()).isEqualTo(new BigDecimal("15.000"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("315.000"));
    }

    @Test
    void createFromForm_dropsItemsWithoutName() {
        Quotation form = new Quotation();
        form.setDate(LocalDate.of(2026, 6, 1));
        QuotationItem nameless = item(1, "10.000", "0.000", null);
        nameless.setProduct(new Product());
        form.setItems(new ArrayList<>(List.of(nameless)));

        when(quotationNumberingService.generateNextNumber(2026)).thenReturn("RYL-2026-001");
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.createFromForm(form);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getSubtotal()).isEqualTo(BigDecimal.ZERO.setScale(3));
        assertThat(result.getTotal()).isEqualTo(BigDecimal.ZERO.setScale(3));
        verify(productRepository, never()).findFirstByNameIgnoreCase(any());
    }

    @Test
    void updateFromForm_replacesItemsAndRecalculates() {
        Quotation existing = new Quotation();
        existing.setId(1L);
        existing.setNumber("RYL-2026-001");
        existing.setStatus(QuotationStatus.DRAFT);
        existing.setLayoutType(LayoutType.CLASSIC);
        existing.setDate(LocalDate.of(2026, 6, 1));
        existing.setItems(new ArrayList<>());

        Quotation form = new Quotation();
        form.setId(1L);
        form.setDate(LocalDate.of(2026, 7, 1));
        form.setStatus(QuotationStatus.SENT);
        form.setLayoutType(LayoutType.MODERN);
        form.setNotes("Revised");
        QuotationItem submitted = item(2, "25.000", "0.000", "5");
        Product submittedProduct = new Product();
        submittedProduct.setName("Chair");
        submitted.setProduct(submittedProduct);
        form.setItems(new ArrayList<>(List.of(submitted)));

        when(quotationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.findFirstByNameIgnoreCase("Chair")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(i -> i.getArgument(0));

        Quotation result = quotationService.updateFromForm(form);

        assertThat(result.getNumber()).isEqualTo("RYL-2026-001");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(result.getStatus()).isEqualTo(QuotationStatus.SENT);
        assertThat(result.getLayoutType()).isEqualTo(LayoutType.MODERN);
        assertThat(result.getNotes()).isEqualTo("Revised");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("50.000"));
        assertThat(result.getTaxAmount()).isEqualTo(new BigDecimal("2.500"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("52.500"));
        verify(quotationNumberingService, never()).generateNextNumber(anyInt());
    }

    @Test
    void updateFromForm_requiresId() {
        assertThatThrownBy(() -> quotationService.updateFromForm(new Quotation()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteById_existsDeletes() {
        when(quotationRepository.existsById(1L)).thenReturn(true);
        quotationService.deleteById(1L);
        verify(quotationRepository).deleteById(1L);
    }

    @Test
    void deleteById_notFoundThrows() {
        when(quotationRepository.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> quotationService.deleteById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}