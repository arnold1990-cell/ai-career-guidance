package com.edurite.bursary.source;

import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WebFallbackBursarySource implements BursarySource {

    @Override
    public List<BursaryResultDto> fetch(BursarySearchRequest request) {
        List<BursaryResultDto> seeded = new ArrayList<>();
        if (request.query() != null && request.query().toLowerCase().contains("science")) {
            seeded.add(new BursaryResultDto(
                    "web-stem-1",
                    "National Science Advancement Bursary",
                    "National Science Fund",
                    "Internet fallback result used when official provider data is sparse.",
                    request.qualificationLevel(),
                    request.region(),
                    request.eligibility(),
                    LocalDate.now().plusMonths(2),
                    "https://example.org/bursaries/science",
                    sourceType(),
                    68
            ));
        }
        if (seeded.isEmpty()) {
            seeded.add(new BursaryResultDto(
                    "web-general-1",
                    "Future Leaders Education Grant",
                    "Education Opportunities Hub",
                    "General internet fallback bursary to avoid empty student experience.",
                    request.qualificationLevel(),
                    request.region(),
                    request.eligibility(),
                    LocalDate.now().plusMonths(1),
                    "https://example.org/bursaries/general",
                    sourceType(),
                    60
            ));
        }
        return seeded;
    }

    @Override
    public String sourceType() {
        return "INTERNET_FALLBACK";
    }
}
