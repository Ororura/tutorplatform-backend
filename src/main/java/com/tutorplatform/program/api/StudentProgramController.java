package com.tutorplatform.program.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.program.application.StudentProgramTopicService;
import com.tutorplatform.program.application.TeacherStudentProgramService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/programs")
public class StudentProgramController implements StudentProgramApi {

    private final TeacherStudentProgramService programService;
    private final StudentProgramTopicService topicService;

    public StudentProgramController(
            TeacherStudentProgramService programService, StudentProgramTopicService topicService) {
        this.programService = programService;
        this.topicService = topicService;
    }

    @Override
    @GetMapping
    public List<StudentProgramSummaryResponse> listPrograms(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return programService.listProgramsForStudent(principal);
    }

    @Override
    @GetMapping("/{studentProgramId}")
    public StudentProgramDetailsResponse getProgram(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentProgramId) {
        return programService.getProgramForStudent(principal, studentProgramId);
    }

    @Override
    @GetMapping("/{studentProgramId}/topics/{topicId}")
    public StudentProgramTopicResponse getTopic(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentProgramId,
            @PathVariable UUID topicId) {
        return topicService.getTopic(principal, studentProgramId, topicId);
    }

    @Override
    @GetMapping("/{studentProgramId}/topics/{topicId}/materials/{materialId}/download")
    public ResponseEntity<byte[]> downloadMaterial(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentProgramId,
            @PathVariable UUID topicId,
            @PathVariable UUID materialId) {
        var download =
                topicService.downloadMaterial(principal, studentProgramId, topicId, materialId);
        String filename = download.filename().replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        ContentDisposition.Builder disposition =
                download.materialType() == LessonMaterialType.IMAGE
                        ? ContentDisposition.inline()
                        : ContentDisposition.attachment();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .contentLength(download.content().length)
                .header(
                        "Content-Disposition",
                        disposition.filename(filename, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-store")
                .body(download.content());
    }
}
