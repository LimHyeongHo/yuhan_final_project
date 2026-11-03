package com.Nbbang.backend.domain.inquiry.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.CommentCreateRequest;
import com.Nbbang.backend.domain.inquiry.dto.InquiryDtos.CreateRequest;
import com.Nbbang.backend.domain.inquiry.entity.Inquiry;
import com.Nbbang.backend.domain.inquiry.repository.InquiryRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;
    @Mock
    private UserAccountRepository userAccountRepository;

    private InquiryService inquiryService;
    private UserAccount buyer;
    private UserAccount otherBuyer;
    private UserAccount seller;
    private UserAccount admin;

    @BeforeEach
    void setUp() {
        inquiryService = new InquiryService(
                inquiryRepository,
                userAccountRepository
        );
        buyer = user("buyer@test.com", "구매자", "ROLE_BUYER");
        otherBuyer = user("other@test.com", "다른 구매자", "ROLE_BUYER");
        seller = user("seller@test.com", "판매자", "ROLE_SELLER");
        admin = user("admin@test.com", "관리자", "ROLE_ADMIN");
    }

    @Test
    void buyerCannotSeeSellerInquiryEvenWhenItIsPublicAndAnswered() {
        Inquiry sellerInquiry = inquiry(1L, seller, false);
        sellerInquiry.setAnswered(true);
        Inquiry ownSecret = inquiry(2L, buyer, true);
        Inquiry otherSecret = inquiry(3L, otherBuyer, true);
        when(userAccountRepository.findById(buyer.getEmail())).thenReturn(Optional.of(buyer));
        when(inquiryRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(sellerInquiry, ownSecret, otherSecret));

        var result = inquiryService.getInquiries(buyer.getEmail(), "ALL");

        assertThat(result).extracting("id").containsExactly(2L);
    }

    @Test
    void buyerCannotOpenSellerInquiryDirectly() {
        Inquiry sellerInquiry = inquiry(1L, seller, false);
        when(userAccountRepository.findById(buyer.getEmail())).thenReturn(Optional.of(buyer));
        when(inquiryRepository.findById(1L)).thenReturn(Optional.of(sellerInquiry));

        assertThatThrownBy(() -> inquiryService.getInquiry(buyer.getEmail(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_ACCESS_DENIED);
    }

    @Test
    void globalNoticeIsVisibleToBuyerAndPinnedFirst() {
        Inquiry buyerInquiry = inquiry(1L, buyer, false);
        Inquiry notice = inquiry(2L, admin, false);
        notice.setNotice(true);
        notice.setAnswered(true);
        when(userAccountRepository.findById(buyer.getEmail())).thenReturn(Optional.of(buyer));
        when(inquiryRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(buyerInquiry, notice));

        var result = inquiryService.getInquiries(buyer.getEmail(), "ALL");

        assertThat(result).extracting("id").containsExactly(2L, 1L);
        assertThat(result.get(0).notice()).isTrue();
    }

    @Test
    void onlyAdminCanCreateGlobalNotice() {
        when(userAccountRepository.findById(seller.getEmail())).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> inquiryService.createInquiry(
                seller.getEmail(),
                new CreateRequest("공지", "내용", false, true)
        ))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        when(userAccountRepository.findById(admin.getEmail())).thenReturn(Optional.of(admin));
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = inquiryService.createInquiry(
                admin.getEmail(),
                new CreateRequest("전체 공지", "공지 내용", true, true)
        );

        assertThat(result.notice()).isTrue();
        assertThat(result.secret()).isFalse();
        assertThat(result.answered()).isTrue();
    }

    @Test
    void adminCanFilterSellerInquiries() {
        when(userAccountRepository.findById(admin.getEmail())).thenReturn(Optional.of(admin));
        when(inquiryRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(inquiry(1L, seller, true), inquiry(2L, buyer, false)));

        var result = inquiryService.getInquiries(admin.getEmail(), "SELLER");

        assertThat(result).extracting("id").containsExactly(1L);
    }

    @Test
    void secretInquiryRejectsAnotherNormalUser() {
        Inquiry secret = inquiry(1L, buyer, true);
        when(userAccountRepository.findById(otherBuyer.getEmail())).thenReturn(Optional.of(otherBuyer));
        when(inquiryRepository.findById(1L)).thenReturn(Optional.of(secret));

        assertThatThrownBy(() -> inquiryService.getInquiry(otherBuyer.getEmail(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_ACCESS_DENIED);
    }

    @Test
    void nonAdminCannotAddComment() {
        when(userAccountRepository.findById(seller.getEmail())).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> inquiryService.addAdminComment(
                seller.getEmail(),
                1L,
                new CommentCreateRequest("답변")
        ))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    void adminCanMarkInquiryAsAnswered() {
        Inquiry inquiry = inquiry(1L, buyer, false);
        when(userAccountRepository.findById(admin.getEmail())).thenReturn(Optional.of(admin));
        when(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));

        var result = inquiryService.updateAnswered(admin.getEmail(), 1L, true);

        assertThat(result.answered()).isTrue();
        assertThat(inquiry.isAnswered()).isTrue();
    }

    private UserAccount user(String email, String nickname, String role) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword("password");
        user.setRole(role);
        return user;
    }

    private Inquiry inquiry(Long id, UserAccount author, boolean secret) {
        Inquiry inquiry = new Inquiry();
        inquiry.setId(id);
        inquiry.setAuthor(author);
        inquiry.setAuthorRole(author.getRole());
        inquiry.setTitle("문의 제목 " + id);
        inquiry.setContent("문의 내용");
        inquiry.setSecret(secret);
        inquiry.setCreatedAt(LocalDateTime.now());
        inquiry.setUpdatedAt(LocalDateTime.now());
        return inquiry;
    }
}
