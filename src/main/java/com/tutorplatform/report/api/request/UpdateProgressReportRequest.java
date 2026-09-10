package com.tutorplatform.report.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(name = "UpdateProgressReportRequest")
public class UpdateProgressReportRequest {

    private String teacherSummary;
    private String nextPeriodPlan;

    @NotNull
    @PositiveOrZero
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    private Long version;

    private boolean teacherSummaryPresent;
    private boolean nextPeriodPlanPresent;

    @Schema(nullable = true)
    public String getTeacherSummary() {
        return teacherSummary;
    }

    public void setTeacherSummary(String teacherSummary) {
        this.teacherSummary = teacherSummary;
        this.teacherSummaryPresent = true;
    }

    @Schema(nullable = true)
    public String getNextPeriodPlan() {
        return nextPeriodPlan;
    }

    public void setNextPeriodPlan(String nextPeriodPlan) {
        this.nextPeriodPlan = nextPeriodPlan;
        this.nextPeriodPlanPresent = true;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonIgnore
    public boolean isTeacherSummaryPresent() {
        return teacherSummaryPresent;
    }

    @JsonIgnore
    public boolean isNextPeriodPlanPresent() {
        return nextPeriodPlanPresent;
    }
}
