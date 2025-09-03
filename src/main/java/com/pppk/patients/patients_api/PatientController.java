package com.pppk.patients.patients_api;

import com.pppk.patients.patients_api.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/patients") // <- prefix
@RequiredArgsConstructor
public class PatientController {
    private final PatientAppService app;

    @PostMapping(consumes="application/json", produces="application/json")
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody CreatePatientRequest r) {
        var resp = app.create(r);
        var loc = URI.create("/api/patients/" + resp.id());
        return ResponseEntity.created(loc).body(resp);
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable Long id) { return app.get(id); }

    @GetMapping
    public Page<PatientResponse> search(@RequestParam(required=false) String surname,
                                        @RequestParam(defaultValue="0") int page,
                                        @RequestParam(defaultValue="20") int size) {
        return app.search(surname, PageRequest.of(page, size));
    }

    // OIB lookup (exact match)
    @GetMapping("/by-oib/{oib}")
    public PatientResponse byOib(@PathVariable String oib) { return app.byOib(oib); }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable Long id, @RequestBody UpdatePatientRequest r) { return app.update(id, r); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { app.delete(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/{id}/history")
    public ResponseEntity<Void> addHistory(@PathVariable Long id, @Valid @RequestBody HistoryRequest r) {
        app.addHistory(id, r); return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/prescriptions")
    public ResponseEntity<Void> prescribe(@PathVariable Long id, @Valid @RequestBody PrescriptionRequest r) {
        app.prescribe(id, r); return ResponseEntity.noContent().build();
    }
}

