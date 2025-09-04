package com.pppk.exams.exams_api;

import com.pppk.exams.exams_api.ExamAppService;
import com.pppk.exams.exams_api.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamAppService app;

    @PostMapping
    public ResponseEntity<ExamResponse> create(@Valid @RequestBody CreateExamRequest r) {
        var resp = app.create(r); // id is server-generated
        var location = ServletUriComponentsBuilder
                .fromCurrentContextPath()       // no user-controlled path/query
                .path("/api/exams/{id}")
                .buildAndExpand(resp.id())
                .toUri();
        return ResponseEntity.created(location).body(resp);
    }

    @GetMapping("/{id}")
    public ExamResponse get(@PathVariable Long id){ return app.get(id); }

    @GetMapping
    public Page<ExamResponse> byPatient(@RequestParam Long patientId,
                                                    @RequestParam(defaultValue="0") int page,
                                                    @RequestParam(defaultValue="20") int size){
        return app.byPatient(patientId, PageRequest.of(page, size));
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<AttachmentResponse> addAttachment(@PathVariable Long id, @Valid @RequestBody AttachmentRequest r) {
        var att = app.addAttachment(id, r);
        var location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/exams/{id}/attachments/{attId}")
                .buildAndExpand(id, att.id())
                .toUri();
        return ResponseEntity.created(location).body(att);
    }

    @GetMapping("/{id}/attachments")
    public List<AttachmentResponse> listAtt(@PathVariable Long id){
        return app.listAttachments(id);
    }

    @GetMapping("/types") public List<ExamTypeResponse> types(){ return app.listTypes(); }
}
