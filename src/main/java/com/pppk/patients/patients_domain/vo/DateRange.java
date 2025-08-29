package com.pppk.patients.patients_domain.vo;

import java.time.LocalDate;

public record DateRange(LocalDate start, LocalDate end) {
    public DateRange {
        if (start == null) throw new IllegalArgumentException("start");
        if (end != null && end.isBefore(start)) throw new IllegalArgumentException("end<start");
    }
}
