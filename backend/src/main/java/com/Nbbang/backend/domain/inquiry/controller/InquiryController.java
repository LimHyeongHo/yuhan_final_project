package com.Nbbang.backend.domain.inquiry.controller;

import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.AnswerRequest;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.CommentCreateRequest;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.CommentResponse;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.CreateRequest;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.DetailResponse;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.SummaryResponse;
import com.Nbbang.backend.domain.inquiry.service.InquiryService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @GetMapping("/api/inquiries")
    public ResponseEntity<List<SummaryResponse>> getInquiries(
            @RequestParam(required = false, defaultValue = "ALL") String authorType,
            HttpSession session
    ) {
        return ResponseEntity.ok(inquiryService.getInquiries(getEmail(session), authorType));
    }

    @GetMapping("/api/inquiries/{id}")
    public ResponseEntity<DetailResponse> getInquiry(
            @PathVariable Long id,
            HttpSession session
    ) {
        return ResponseEntity.ok(inquiryService.getInquiry(getEmail(session), id));
    }

    @PostMapping("/api/inquiries")
    public ResponseEntity<DetailResponse> createInquiry(
            @Valid @RequestBody CreateRequest request,
            HttpSession session
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inquiryService.createInquiry(getEmail(session), request));
    }

    @PostMapping("/api/admin/inquiries/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentCreateRequest request,
            HttpSession session
    ) {
        return ResponseEntity.ok(inquiryService.addAdminComment(getEmail(session), id, request));
    }

    @PatchMapping("/api/admin/inquiries/{id}/answer")
    public ResponseEntity<DetailResponse> updateAnswered(
            @PathVariable Long id,
            @RequestBody AnswerRequest request,
            HttpSession session
    ) {
        return ResponseEntity.ok(
                inquiryService.updateAnswered(getEmail(session), id, request.answered())
        );
    }

    @DeleteMapping("/api/admin/inquiries/{id}")
    public ResponseEntity<Void> deleteInquiry(@PathVariable Long id, HttpSession session) {
        inquiryService.deleteInquiry(getEmail(session), id);
        return ResponseEntity.noContent().build();
    }

    private String getEmail(HttpSession session) {
        String email = (String) session.getAttribute("userId");
        if (email == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return email;
    }
}
