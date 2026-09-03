package com.project.Anusha.util;

import com.project.Anusha.repository.RfqRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RfqCodeGenerator {

    private final RfqRepository rfqRepository;
    private final AtomicLong sequence = new AtomicLong(0);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private int currentYear = LocalDate.now().getYear();

    public RfqCodeGenerator(RfqRepository rfqRepository) {
        this.rfqRepository = rfqRepository;
    }

    private synchronized void ensureInitialized() {
        int year = LocalDate.now().getYear();
        if (!initialized.get() || year != currentYear) {
            LocalDateTime startOfYear = LocalDate.now().withDayOfYear(1).atStartOfDay();
            LocalDateTime endOfYear = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear()).atTime(23, 59, 59);

            try {
                long count = rfqRepository.countByCreatedAtBetween(startOfYear, endOfYear);
                sequence.set(count);
            } catch (Exception e) {
                // In case tables are empty or initializing
                sequence.set(0);
            }
            currentYear = year;
            initialized.set(true);
        }
    }

    /**
     * Generates a thread-safe, concurrency-safe human-readable RFQ code.
     * Format: RFQ-YYYY-000001 (e.g. RFQ-2026-000001)
     */
    public synchronized String generateRfqCode() {
        ensureInitialized();

        String code;
        do {
            long nextSeq = sequence.incrementAndGet();
            code = String.format("RFQ-%d-%06d", currentYear, nextSeq);
        } while (rfqRepository.findByRfqCode(code).isPresent());

        return code;
    }
}
