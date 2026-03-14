package com.edurite.ai.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class UniversitySourcesAnalysisRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsMoreThanTenUrls() {
        var urls = IntStream.range(0, 11).mapToObj(i -> "https://www.unisa.ac.za/" + i).toList();
        var request = new UniversitySourcesAnalysisRequest(urls, null, null, null, 10);

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
