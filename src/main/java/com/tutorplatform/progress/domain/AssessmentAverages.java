package com.tutorplatform.progress.domain;

import java.math.BigDecimal;

public record AssessmentAverages(
    BigDecimal understanding,
    BigDecimal independence,
    BigDecimal practice,
    BigDecimal homework
) {
}
