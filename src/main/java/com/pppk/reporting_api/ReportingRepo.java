package com.pppk.reporting_api;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportingRepo {
    private final JdbcTemplate jdbc;

    public List<PatientExportRow> exportAll() {
        return jdbc.query("""
        select id, first_name, last_name, oib, birth_date, sex, created_at, updated_at
        from v_patient_basic_export
        order by id
        """,
                (rs, i) -> new PatientExportRow(
                        rs.getLong("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("oib"),
                        rs.getDate("birth_date").toLocalDate(),
                        rs.getString("sex"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class),
                        rs.getObject("updated_at", java.time.OffsetDateTime.class)
                )
        );
    }
}
