package com.Nbbang.backend.domain.inquiry.dto;

import com.Nbbang.backend.domain.inquiry.entity.Inquiry;
import com.Nbbang.backend.domain.inquiry.entity.InquiryComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class InquiryDtos {

    private InquiryDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 5000) String content,
            boolean secret,
            boolean notice
    ) {
    }

    public record CommentCreateRequest(
            @NotBlank @Size(max = 2000) String content
    ) {
    }

    public record AnswerRequest(boolean answered) {
    }

    public record CommentResponse(
            Long id,
            String adminNickname,
            String content,
            LocalDateTime createdAt
    ) {
        public static CommentResponse from(InquiryComment comment) {
            return new CommentResponse(
                    comment.getId(),
                    comment.getAdmin().getNickname(),
                    comment.getContent(),
                    comment.getCreatedAt()
            );
        }
    }

    public record SummaryResponse(
            Long id,
            String title,
            boolean secret,
            boolean answered,
            boolean notice,
            String authorNickname,
            String authorRole,
            int commentCount,
            LocalDateTime createdAt
    ) {
        public static SummaryResponse from(Inquiry inquiry) {
            return new SummaryResponse(
                    inquiry.getId(),
                    inquiry.getTitle(),
                    inquiry.isSecret(),
                    inquiry.isAnswered(),
                    inquiry.isNotice(),
                    inquiry.getAuthor().getNickname(),
                    inquiry.getAuthorRole(),
                    inquiry.getComments().size(),
                    inquiry.getCreatedAt()
            );
        }
    }

    public record DetailResponse(
            Long id,
            String title,
            String content,
            boolean secret,
            boolean answered,
            boolean notice,
            String authorNickname,
            String authorRole,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<CommentResponse> comments
    ) {
        public static DetailResponse from(Inquiry inquiry) {
            return new DetailResponse(
                    inquiry.getId(),
                    inquiry.getTitle(),
                    inquiry.getContent(),
                    inquiry.isSecret(),
                    inquiry.isAnswered(),
                    inquiry.isNotice(),
                    inquiry.getAuthor().getNickname(),
                    inquiry.getAuthorRole(),
                    inquiry.getCreatedAt(),
                    inquiry.getUpdatedAt(),
                    inquiry.getComments().stream().map(CommentResponse::from).toList()
            );
        }
    }
}
