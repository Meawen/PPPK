package com.pppk.patients.patients_domain.Model;

import com.pppk.patients.patients_domain.vo.DateRange;
import lombok.Getter;

@Getter
public final class MedicalHistoryEntry {
    private final Long id;
    private final String diseaseName;
    private final DateRange period;
    public MedicalHistoryEntry(Long id, String diseaseName, DateRange period) {
        if (diseaseName == null || diseaseName.isBlank()) throw new IllegalArgumentException("diseaseName");
        this.id = id; this.diseaseName = diseaseName; this.period = period;
    }
}
