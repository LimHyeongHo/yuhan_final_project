package com.Nbbang.backend.domain.report.service;

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.chat.entity.ChatMessage;
import com.Nbbang.backend.domain.chat.entity.ChatRoom;
import com.Nbbang.backend.domain.chat.entity.MessageType;
import com.Nbbang.backend.domain.chat.repository.ChatMessageRepository;
import com.Nbbang.backend.domain.chat.repository.ChatRoomRepository;
import com.Nbbang.backend.domain.notification.entity.Notification;
import com.Nbbang.backend.domain.notification.repository.NotificationRepository;
import com.Nbbang.backend.domain.product.entity.Participation;
import com.Nbbang.backend.domain.product.repository.ParticipationRepository;
import com.Nbbang.backend.domain.report.dto.ChatReportCreateRequest;
import com.Nbbang.backend.domain.report.entity.ChatReport;
import com.Nbbang.backend.domain.report.entity.UserReportCount;
import com.Nbbang.backend.domain.report.repository.ChatReportRepository;
import com.Nbbang.backend.domain.report.repository.UserReportCountRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 채팅 신고 처리
// 신고를 저장하고 신고당한 사람의 누적 신고 수를 1 올림
@Service
@RequiredArgsConstructor
public class ChatReportService {

    // 고를 수 있는 신고 사유 (프론트 신고 창과 같은 목록)
    private static final Set<String> ALLOWED_REASONS = Set.of(
            "개인 거래 유도",
            "선입금 요청",
            "욕설·비방",
            "기타");

    private static final int MAX_DETAIL_LENGTH = 1000;

    // 사람이 보낸 글, 사진만 신고 가능 (입장, 퇴장 안내는 제외)
    private static final Set<MessageType> REPORTABLE_TYPES = Set.of(MessageType.CHAT, MessageType.IMAGE);

    // 관리자가 바꿀 수 있는 상태 (접수, 반려)
    private static final Set<String> MANAGEABLE_STATUSES = Set.of("RECEIVED", "REJECTED");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserAccountRepository userAccountRepository;
    private final ChatReportRepository chatReportRepository;
    private final UserReportCountRepository userReportCountRepository;
    private final ParticipationRepository participationRepository;
    private final NotificationRepository notificationRepository;

    // 신고 접수: 아래 순서로 검사한 뒤 저장
    // (사유 → 내 채팅방 → 이 방 메시지 → 상대 메시지 → 글이나 사진 → 중복 신고)
    @Transactional
    public void createReport(Long roomId, String reporterEmail, ChatReportCreateRequest request) {
        // 사유는 정해진 목록만, 상세는 1000자 이하 (기타는 상세 필수)
        if (request == null || request.messageId() == null) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        String detail = request.detail() == null ? null : request.detail().trim();
        if (!ALLOWED_REASONS.contains(reason) || (detail != null && detail.length() > MAX_DETAIL_LENGTH)) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }
        if ("기타".equals(reason) && (detail == null || detail.isBlank())) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        // 내가 참여한 채팅방인지 확인
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!reporterEmail.equals(room.getBuyerEmail()) && !reporterEmail.equals(room.getSellerEmail())) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // 이 방의 메시지인지, 상대가 보낸 건지, 글이나 사진인지 확인
        ChatMessage message = chatMessageRepository.findById(request.messageId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
        if (!roomId.equals(message.getRoomId())
                || reporterEmail.equals(message.getSenderEmail())
                || !REPORTABLE_TYPES.contains(message.getType())) {
            throw new CustomException(ErrorCode.CHAT_REPORT_INVALID_MESSAGE);
        }

        // 같은 메시지를 이미 신고했으면 거절
        if (chatReportRepository.existsByReporter_EmailAndMessage_Id(reporterEmail, message.getId())) {
            throw new CustomException(ErrorCode.CHAT_REPORT_DUPLICATE);
        }

        UserAccount reporter = userAccountRepository.findById(reporterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        // 신고당한 사람은 프론트가 보낸 값이 아니라 DB의 메시지 보낸 사람으로 정함 (조작 방지)
        UserAccount reported = userAccountRepository.findById(message.getSenderEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        ChatReport report = new ChatReport();
        report.setMessage(message);
        report.setReporter(reporter);
        report.setReported(reported);
        report.setReason(reason);
        report.setDetail(detail == null || detail.isBlank() ? null : detail);
        try {
            // 동시에 두 번 눌러도 DB의 중복 금지 규칙 때문에 한 번만 저장됨
            chatReportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.CHAT_REPORT_DUPLICATE);
        }

        // 신고당한 사람의 누적 채팅 신고 수 +1 (user_report_counts 테이블)
        UserReportCount reportCount = userReportCountRepository.findById(reported.getEmail())
                .orElseGet(() -> {
                    UserReportCount newReportCount = new UserReportCount();
                    newReportCount.setMemberEmail(reported.getEmail());
                    return newReportCount;
                });
        reportCount.setChatReportCount(reportCount.getChatReportCount() + 1);
        userReportCountRepository.save(reportCount);

        // 새 신고가 들어오면 관리자에게 알림
        userAccountRepository.findByRoleOrderByCreatedAtDesc("ROLE_ADMIN")
                .forEach(admin -> notificationRepository.save(new Notification(
                        admin.getEmail(),
                        "새 채팅 신고가 들어왔습니다. 신고 대상: " + reported.getNickname() + ", 사유: " + reason)));
    }

    // 관리자 처리 (검토 대기 상태일 때만 접수나 반려 가능)
    @Transactional
    public void updateReportStatus(Long reportId, String status) {
        if (status == null || !MANAGEABLE_STATUSES.contains(status)) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }

        ChatReport report = chatReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_REPORT_NOT_FOUND));
        if (!"PENDING".equals(report.getStatus())) {
            throw new CustomException(ErrorCode.ADMIN_APPROVAL_ALREADY_PROCESSED);
        }
        report.setStatus(status);

        // 접수하면 신고당한 사람에게 경고 알림, 관리자에게도 알림 (반려는 알림 없음)
        if ("RECEIVED".equals(status)) {
            UserAccount reported = report.getReported();
            notificationRepository.save(new Notification(
                    reported.getEmail(),
                    "채팅에서 보내신 메시지에 대한 신고가 관리자에 의해 접수되었습니다. "
                            + "같은 신고가 반복되면 서비스 이용이 제한될 수 있으니 주의해 주세요."));

            userAccountRepository.findByRoleOrderByCreatedAtDesc("ROLE_ADMIN")
                    .forEach(admin -> notificationRepository.save(new Notification(
                            admin.getEmail(),
                            "채팅 신고가 접수 처리되었습니다. 신고 대상: " + reported.getNickname())));
        }
    }

    // 관리자 신고 기록 (상품 신고와 같은 형식이라 화면에서 함께 보여줌)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllReports() {
        return chatReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(report -> {
                    ChatMessage message = report.getMessage();
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("type", "CHAT");
                    result.put("id", report.getChatReportId());
                    result.put("reporterEmail", report.getReporter().getEmail());
                    result.put("reporterName", report.getReporter().getNickname());
                    result.put("reportedEmail", report.getReported().getEmail());
                    result.put("reportedName", report.getReported().getNickname());
                    result.put("roomId", message.getRoomId());
                    // 보낸 사람이 지운 메시지도 원문을 보여줌 (DB에 남아 있음)
                    result.put("messageContent", message.getType() == MessageType.IMAGE ? "[사진] " + message.getContent() : message.getContent());
                    result.put("messageDeleted", message.isDeleted());
                    result.put("messageSentAt", message.getSentAt() == null ? null : message.getSentAt().toString());
                    result.put("reason", report.getReason());
                    result.put("detail", report.getDetail());
                    result.put("statusCode", report.getStatus());
                    result.put("reportedAt", report.getCreatedAt().toLocalDate().toString());
                    result.put("status", "PENDING".equals(report.getStatus()) ? "검토 대기" : report.getStatus());
                    return result;
                })
                .toList();
    }

    // 채팅 신고를 1건 이상 받은 구매자 목록
    // 현재 참여는 모집 중인 공동구매, 누적 참여는 지금까지 참여한 전체 (취소한 건 제외)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReportedBuyers() {
        return userReportCountRepository.findAll().stream()
                .filter(count -> count.getChatReportCount() > 0)
                .map(count -> userAccountRepository.findById(count.getMemberEmail())
                        .filter(user -> "ROLE_BUYER".equals(user.getRole()) || "ROLE_USER".equals(user.getRole()))
                        .map(buyer -> {
                            List<Participation> participations =
                                    participationRepository.findByMember_EmailOrderByJoinDateDesc(buyer.getEmail());
                            long currentCount = participations.stream()
                                    .filter(p -> "OPEN".equals(p.getProduct().getStatus()))
                                    .count();

                            Map<String, Object> result = new LinkedHashMap<>();
                            // 화면에는 이메일 일부를 가려서 보여줌
                            result.put("email", maskEmail(buyer.getEmail()));
                            result.put("buyerEmail", buyer.getEmail());
                            result.put("nickname", buyer.getNickname());
                            result.put("currentParticipationCount", currentCount);
                            result.put("totalParticipationCount", participations.size());
                            result.put("chatReportCount", count.getChatReportCount());
                            result.put("pendingChatReportCount",
                                    chatReportRepository.countByReported_EmailAndStatus(buyer.getEmail(), "PENDING"));
                            return result;
                        }))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    // 이메일 앞 4글자만 보이게 가리기 (판매자 목록과 같은 방식)
    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex > 4) return email.substring(0, 4) + "***" + email.substring(atIndex);
        if (atIndex > 0) return email.substring(0, atIndex) + "***" + email.substring(atIndex);
        if (email.length() > 4) return email.substring(0, 4) + "***";
        return email;
    }
}
