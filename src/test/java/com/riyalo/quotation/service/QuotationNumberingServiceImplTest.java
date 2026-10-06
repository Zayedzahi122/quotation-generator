package com.riyalo.quotation.service;

import com.riyalo.quotation.repository.QuotationRepository;
import com.riyalo.quotation.service.impl.QuotationNumberingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotationNumberingServiceImplTest {

    @Mock
    private QuotationRepository quotationRepository;

    private QuotationNumberingServiceImpl numberingService;

    @BeforeEach
    void setUp() {
        numberingService = new QuotationNumberingServiceImpl(quotationRepository);
    }

    @Test
    void generateNextNumber_firstInYear() {
        when(quotationRepository.findMaxNumberForYear(2026)).thenReturn(null);
        String result = numberingService.generateNextNumber(2026);
        assertThat(result).isEqualTo("RYL-2026-001");
    }

    @Test
    void generateNextNumber_incrementExisting() {
        when(quotationRepository.findMaxNumberForYear(2026)).thenReturn("RYL-2026-001");
        String result = numberingService.generateNextNumber(2026);
        assertThat(result).isEqualTo("RYL-2026-002");
    }

    @Test
    void generateNextNumber_handlesDoubleDigit() {
        when(quotationRepository.findMaxNumberForYear(2026)).thenReturn("RYL-2026-010");
        String result = numberingService.generateNextNumber(2026);
        assertThat(result).isEqualTo("RYL-2026-011");
    }

    @Test
    void generateNextNumber_handlesLargerSequence() {
        when(quotationRepository.findMaxNumberForYear(2025)).thenReturn("RYL-2025-099");
        String result = numberingService.generateNextNumber(2025);
        assertThat(result).isEqualTo("RYL-2025-100");
    }
}