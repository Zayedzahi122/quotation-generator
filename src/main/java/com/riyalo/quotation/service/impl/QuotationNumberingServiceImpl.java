package com.riyalo.quotation.service.impl;

import com.riyalo.quotation.repository.QuotationRepository;
import com.riyalo.quotation.service.QuotationNumberingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class QuotationNumberingServiceImpl implements QuotationNumberingService {

    private static final Pattern YEAR_SUFFIX_PATTERN = Pattern.compile("RYL-(\\d{4})-(\\d{3})");

    private final QuotationRepository quotationRepository;

    @Override
    public String generateNextNumber(int year) {
        String maxNumber = quotationRepository.findMaxNumberForYear(year);
        int nextSeq = 1;
        if (maxNumber != null && !maxNumber.isEmpty()) {
            Matcher matcher = YEAR_SUFFIX_PATTERN.matcher(maxNumber);
            if (matcher.find()) {
                try {
                    nextSeq = Integer.parseInt(matcher.group(2)) + 1;
                } catch (NumberFormatException e) {
                    nextSeq = 1;
                }
            }
        }
        return String.format("RYL-%d-%03d", year, nextSeq);
    }
}