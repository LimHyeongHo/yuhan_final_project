package com.Nbbang.backend.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // ========== 공통 ==========
    // NETWORK_TIMEOUT(408)은 프론트 axios/fetch 레벨에서 처리 → 백엔드 enum에서 제외
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요"),
    TOO_MANY_REQUESTS(429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요"),
    VALIDATION_FAILED(400, "입력값을 확인해주세요"),
    FILE_SIZE_EXCEEDED(400, "파일 크기는 5MB 이하여야 합니다"),

    // ========== 인증 (AUTH) ==========
    AUTH_SESSION_EXPIRED(401, "세션이 만료되었습니다. 다시 로그인해주세요"),
    AUTH_UNAUTHORIZED(401, "로그인이 필요합니다"),
    AUTH_ACCESS_DENIED(403, "접근 권한이 없습니다"),
    AUTH_INVALID_CREDENTIALS(401, "이메일 또는 비밀번호를 확인해주세요"),
    AUTH_ACCOUNT_LOCKED(403, "계정이 잠겼습니다. 잠시 후 다시 시도해주세요"),
    AUTH_UNREGISTERED_DEVICE(403, "등록되지 않은 기기입니다"),
    AUTH_SIGNATURE_VERIFICATION_FAILED(401, "인증에 실패했습니다. 다시 로그인해주세요"),
    AUTH_WITHDRAWN_ACCOUNT(403, "탈퇴된 계정입니다. 고객센터에 문의해주세요"),
    AUTH_PORTONE_SERVER_ERROR(502, "본인인증에 실패했습니다. 다시 시도해주세요"),

    // ========== 회원 (MEMBER) ==========
    MEMBER_DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다"),
    MEMBER_DUPLICATE_CI(409, "이미 가입된 계정입니다"),

    // ========== 공동구매 (PURCHASE) ==========
    // _LOAD_FAILED 계열은 DB 연결 실패 등 서버 내부 오류로 조회 자체가 불가능할 때만 사용
    PURCHASE_LIST_LOAD_FAILED(500, "목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    PURCHASE_FULL(409, "모집이 완료된 공동구매입니다"),
    PURCHASE_ALREADY_JOINED(409, "이미 참여 중인 공동구매입니다"),

    // ========== 결제 (PAYMENT) ==========(총4줄 추가됨)
    PAYMENT_INVALID_AMOUNT(400, "결제 금액이 올바르지 않습니다"),
    PAYMENT_ORDER_NOT_FOUND(404, "유효하지 않은 주문입니다"),
    PAYMENT_AMOUNT_MISMATCH(400, "결제 금액이 일치하지 않습니다"),
    PAYMENT_CONFIRM_FAILED(502, "결제 승인에 실패했습니다. 잠시 후 다시 시도해주세요"),

    // ========== 상품 (PRODUCT) ==========
    //[추가]
    PRODUCT_NOT_FOUND(404, "존재하지 않는 상품입니다"),
    PRODUCT_INVALID_FILE_FORMAT(400, "PNG, JPG 파일만 업로드 가능합니다"),
    PRODUCT_FILE_SIZE_EXCEEDED(400, "파일 크기는 5MB 이하여야 합니다"),
    PRODUCT_INVALID_DEADLINE(400, "마감일은 오늘 이후여야 합니다"),
    PRODUCT_BARCODE_API_ERROR(502, "도서 정보를 불러오지 못했습니다. 직접 입력해주세요"),
    PRODUCT_DUPLICATE(409, "이미 등록된 상품입니다"),
    PRODUCT_REGISTER_FAILED(500, "상품 등록에 실패했습니다. 잠시 후 다시 시도해주세요"),
    PRODUCT_DELETE_FAILED(500, "삭제에 실패했습니다. 잠시 후 다시 시도해주세요"),
    PRODUCT_UPDATE_FAILED(500, "수정에 실패했습니다. 잠시 후 다시 시도해주세요"),
    PRODUCT_CANNOT_DELETE_WITH_PARTICIPANTS(409, "이미 참여자가 있는 프로젝트는 삭제할 수 없습니다"),
    PRODUCT_CANNOT_MODIFY_COMPLETED(409, "목표 달성 완료된 프로젝트는 수정하거나 삭제할 수 없습니다"),

    // ========== 판매자 (SELLER) ==========
    SELLER_STATISTICS_LOAD_FAILED(500, "통계 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    SELLER_SALES_LIST_LOAD_FAILED(500, "판매 현황을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    SELLER_NOTIFICATION_LOAD_FAILED(500, "알림을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    SELLER_PRODUCT_LIST_LOAD_FAILED(500, "목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    SELLER_ANALYTICS_LOAD_FAILED(500, "데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),

    // ========== 관리자 (ADMIN) ==========
    ADMIN_STATISTICS_LOAD_FAILED(500, "통계 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_SECURITY_LOG_LOAD_FAILED(500, "로그를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_DETAIL_LOAD_FAILED(500, "상세 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_FORGERY_TRACE_LOAD_FAILED(500, "위변조 추적 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_LOG_EXPORT_FAILED(500, "로그 내보내기에 실패했습니다. 다시 시도해주세요"),
    ADMIN_APPROVAL_QUEUE_LOAD_FAILED(500, "대기열을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_PERMISSION_GRANT_FAILED(500, "권한 부여에 실패했습니다. 다시 시도해주세요"),
    ADMIN_APPROVAL_ALREADY_PROCESSED(409, "이미 처리된 신청입니다"),
    ADMIN_TRANSACTION_ALREADY_PROCESSED(409, "이미 처리된 거래입니다"),
    ADMIN_MONITORING_LOAD_FAILED(500, "모니터링 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_REVIEW_LOAD_FAILED(500, "심사 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_CHART_LOAD_FAILED(500, "차트 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_SYNC_FAILED(500, "서버 동기화에 실패했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_PRODUCT_LIST_LOAD_FAILED(500, "상품 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_TRANSACTION_APPROVE_FAILED(500, "거래 승인에 실패했습니다. 다시 시도해주세요"),
    ADMIN_TRANSACTION_REJECT_FAILED(500, "거래 거절에 실패했습니다. 다시 시도해주세요"),
    ADMIN_HISTORY_LOAD_FAILED(500, "히스토리를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_ANOMALY_DETAIL_LOAD_FAILED(500, "이상 감지 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_SYNC_LOG_LOAD_FAILED(500, "동기화 로그를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),

    // ========== 채팅 (CHAT) ==========
    // WebSocket 에러(WS_CONNECT_FAILED, WS_DISCONNECTED, WS_SEND_FAILED)는 STOMP 프레임으로 처리
    // → HTTP 응답이 아니므로 이 enum에 포함하지 않음 (SharedChatPage WebSocket 핸들러 참고)
    CHAT_ROOM_NOT_FOUND(404, "채팅방을 찾을 수 없습니다"),
    CHAT_ACCESS_DENIED(403, "접근 권한이 없는 채팅방입니다"),
    CHAT_HISTORY_LOAD_FAILED(500, "채팅 내역을 불러오지 못했습니다"),
    CHAT_INVALID_FILE_FORMAT(400, "이미지 파일(PNG/JPG/GIF/WEBP)만 업로드 가능합니다"),
    CHAT_FILE_SIZE_EXCEEDED(400, "파일 크기는 5MB 이하여야 합니다"),

    // ========== 구매자 (BUYER) ==========
    BUYER_PRODUCT_LIST_LOAD_FAILED(500, "목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),

    // ========== 마이페이지 (MYPAGE) ==========
    MYPAGE_PROFILE_LOAD_FAILED(500, "회원 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_ORDER_LIST_LOAD_FAILED(500, "참여 내역을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_ORDER_DETAIL_LOAD_FAILED(500, "상세 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_PROJECT_LIST_LOAD_FAILED(500, "프로젝트 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_SCRAP_LIST_LOAD_FAILED(500, "스크랩 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_SCRAP_DELETE_FAILED(500, "스크랩 삭제에 실패했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_SETTINGS_UPDATE_FAILED(500, "변경사항 저장에 실패했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_SETTLEMENT_LOAD_FAILED(500, "정산 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_SETTLEMENT_NO_ACCOUNT(400, "출금을 위한 정산 계좌가 등록되어 있지 않습니다"),
    MYPAGE_SETTLEMENT_WITHDRAW_FAILED(500, "출금 신청에 실패했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_SETTLEMENT_ACCOUNT_UPDATE_FAILED(500, "계좌 변경에 실패했습니다. 잠시 후 다시 시도해주세요");

    private final int status;
    private final String message;

    ErrorCode(final int status, final String message) {
        this.status = status;
        this.message = message;
    }
}
