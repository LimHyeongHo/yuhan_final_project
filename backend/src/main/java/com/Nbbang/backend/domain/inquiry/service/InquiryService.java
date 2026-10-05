package com.Nbbang.backend.domain.inquiry.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.CommentCreateRequest;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.CommentResponse;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.CreateRequest;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.DetailResponse;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.SummaryResponse;
import com.Nbbang.backend.domain.inquiry.entity.Inquiry;
import com.Nbbang.backend.domain.inquiry.entity.InquiryComment;
import com.Nbbang.backend.domain.inquiry.repository.InquiryRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_SELLER = "ROLE_SELLER";
    private static final String ROLE_BUYER = "ROLE_BUYER";
    private static final String ROLE_SELLER_PENDING = "ROLE_SELLER_PENDING";

    private final InquiryRepository inquiryRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional(readOnly = true)
    public List<SummaryResponse> getInquiries(String requesterEmail, String authorType) {
        UserAccount requester = getUser(requesterEmail);
        boolean admin = ROLE_ADMIN.equals(requester.getRole());

        return inquiryRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(inquiry -> admin || !inquiry.isSecret()
                        || inquiry.getAuthor().getEmail().equals(requesterEmail))
                .filter(inquiry -> canViewAuthorRole(requester.getRole(), inquiry))
                .filter(inquiry -> !admin || matchesAuthorType(inquiry, authorType))
                .sorted(Comparator.comparing(Inquiry::isNotice).reversed())
                .map(SummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DetailResponse getInquiry(String requesterEmail, Long inquiryId) {
        UserAccount requester = getUser(requesterEmail);
        Inquiry inquiry = getInquiryEntity(inquiryId);
        boolean canRead = ROLE_ADMIN.equals(requester.getRole())
                || (canViewAuthorRole(requester.getRole(), inquiry)
                && (!inquiry.isSecret() || inquiry.getAuthor().getEmail().equals(requesterEmail)));

        if (!canRead) {
            throw new CustomException(ErrorCode.INQUIRY_ACCESS_DENIED);
        }
        return DetailResponse.from(inquiry);
    }

    @Transactional
    public DetailResponse createInquiry(String authorEmail, CreateRequest request) {
        UserAccount author = getUser(authorEmail);
        validateSupportedRole(author.getRole());
        if (request.notice() && !ROLE_ADMIN.equals(author.getRole())) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        Inquiry inquiry = new Inquiry();
        inquiry.setAuthor(author);
        inquiry.setAuthorRole(author.getRole());
        inquiry.setTitle(request.title().trim());
        inquiry.setContent(request.content().trim());
        inquiry.setSecret(request.notice() ? false : request.secret());
        inquiry.setAnswered(request.notice());
        inquiry.setNotice(request.notice());

        return DetailResponse.from(inquiryRepository.save(inquiry));
    }

    @Transactional
    public CommentResponse addAdminComment(
            String adminEmail,
            Long inquiryId,
            CommentCreateRequest request
    ) {
        UserAccount admin = requireAdmin(adminEmail);
        Inquiry inquiry = getInquiryEntity(inquiryId);
        if (inquiry.isNotice()) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        InquiryComment comment = new InquiryComment();
        comment.setInquiry(inquiry);
        comment.setAdmin(admin);
        comment.setContent(request.content().trim());
        inquiry.getComments().add(comment);
        inquiryRepository.flush();

        return CommentResponse.from(comment);
    }

    @Transactional
    public DetailResponse updateAnswered(String adminEmail, Long inquiryId, boolean answered) {
        requireAdmin(adminEmail);
        Inquiry inquiry = getInquiryEntity(inquiryId);
        if (inquiry.isNotice()) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }
        inquiry.setAnswered(answered);
        return DetailResponse.from(inquiry);
    }

    @Transactional
    public void deleteInquiry(String adminEmail, Long inquiryId) {
        requireAdmin(adminEmail);
        inquiryRepository.delete(getInquiryEntity(inquiryId));
    }

    private Inquiry getInquiryEntity(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    private UserAccount getUser(String email) {
        return userAccountRepository.findById(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private UserAccount requireAdmin(String email) {
        UserAccount user = getUser(email);
        if (!ROLE_ADMIN.equals(user.getRole())) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
        return user;
    }

    private void validateSupportedRole(String role) {
        if (!ROLE_ADMIN.equals(role)
                && !ROLE_SELLER.equals(role)
                && !ROLE_BUYER.equals(role)
                && !ROLE_SELLER_PENDING.equals(role)) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    private boolean matchesAuthorType(Inquiry inquiry, String authorType) {
        if (inquiry.isNotice()) {
            return true;
        }
        String role = inquiry.getAuthorRole();
        if (authorType == null || authorType.isBlank() || "ALL".equalsIgnoreCase(authorType)) {
            return true;
        }
        if ("SELLER".equalsIgnoreCase(authorType)) {
            return ROLE_SELLER.equals(role);
        }
        if ("BUYER".equalsIgnoreCase(authorType)) {
            return ROLE_BUYER.equals(role) || ROLE_SELLER_PENDING.equals(role);
        }
        throw new CustomException(ErrorCode.VALIDATION_FAILED);
    }

    private boolean canViewAuthorRole(String requesterRole, Inquiry inquiry) {
        if (inquiry.isNotice() || ROLE_ADMIN.equals(requesterRole)) {
            return true;
        }
        boolean buyer = ROLE_BUYER.equals(requesterRole) || ROLE_SELLER_PENDING.equals(requesterRole);
        return !buyer || !ROLE_SELLER.equals(inquiry.getAuthorRole());
    }
}
