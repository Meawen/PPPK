package com.pppk.reporting_api;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/reporting") @RequiredArgsConstructor
public class ReportingController {
    private final ReportingRepo repo;

    @GetMapping(value="/patients/export.csv", produces="text/csv")
    public void exportPatients(HttpServletResponse res) throws IOException {
        res.setHeader("Content-Disposition", "attachment; filename=patients.csv");
        var w = res.getWriter();
        w.println("id,first_name,last_name,oib,birth_date,sex,created_at,updated_at");
        for (var r : repo.exportAll()) {
            w.printf("%d,%s,%s,%s,%s,%s,%s,%s%n",
                    r.id(), esc(r.firstName()), esc(r.lastName()), r.oib(),
                    r.birthDate(), r.sex(), r.createdAt(), r.updatedAt());
        }
    }
    private static String esc(String s){ return s==null?"":s.replace("\"","\"\""); }
}
