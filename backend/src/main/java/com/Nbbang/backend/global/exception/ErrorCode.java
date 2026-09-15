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
    MEMBER_NOT_FOUND(404, "존재하지 않는 회원입니다"),
    // [UI-RQ-002][fix/seller-page] 탈퇴 회원(soft-delete, status=WITHDRAWN)과 미존재 회원을 프론트에서 다른 안내로 구분하기 위한 별도 코드
    MEMBER_WITHDRAWN(404, "탈퇴한 회원입니다"),

    // ========== 공동구매 (PURCHASE) ==========
    // _LOAD_FAILED 계열은 DB 연결 실패 등 서버 내부 오류로 조회 자체가 불가능할 때만 사용
    PURCHASE_LIST_LOAD_FAILED(500, "목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    PURCHASE_FULL(409, "모집이 완료된 공동구매입니다"),
    PURCHASE_ALREADY_JOINED(409, "이미 참여 중인 공동구매입니다"),
    PURCHASE_DEADLINE_PASSED(409, "마감된 공동구매입니다"),
    // ===== 0906 문건우 수정 시작 =====
    // PRD-RQ-001: 본인 소유가 아니거나 존재하지 않는 참여 건에 대한 취소 요청
    PARTICIPATION_NOT_FOUND(404, "참여 내역을 찾을 수 없습니다"),
    // ===== 0906 문건우 수정 끝 =====

    // ========== 결제 (PAYMENT) ==========(총4줄 추가됨)
    PAYMENT_INVALID_AMOUNT(400, "결제 금액이 올바르지 않습니다"),
    PAYMENT_ORDER_NOT_FOUND(404, "유효하지 않은 주문입니다"),
    PAYMENT_AMOUNT_MISMATCH(400, "결제 금액이 일치하지 않습니다"),
    PAYMENT_CANCELLED(400, "결제가 취소되었습니다."),
    PAYMENT_CONFIRM_FAILED(502, "결제 승인에 실패했습니다. 잠시 후 다시 시도해주세요"),
    // ===== 0906 문건우 수정 시작 =====
    // PAY-RQ-001: Toss 취소 API 호출 실패 (네트워크 오류/명시적 거부 모두 포함, 재시도 가능)
    PAYMENT_REFUND_FAILED(502, "환불 처리에 실패했습니다. 잠시 후 다시 시도해주세요"),
    // PAY-RQ-001: Toss 승인은 끝났지만 참여 확정에 실패해 자동 환불된 건 (성공 콜백을 성공으로 응답하면 안 됨)
    PAYMENT_JOIN_FAILED(409, "결제는 승인됐지만 참여 확정에 실패해 자동 환불되었습니다. 다시 시도해주세요"),
    // PRD-RQ-001: 동일 결제에 대한 환불이 이미 진행 중 (동시 중복 취소 요청 차단)
    PAYMENT_CANCEL_IN_PROGRESS(409, "환불이 이미 진행 중입니다. 잠시 후 상태를 확인해주세요"),
    // PAY-RQ-001: Toss 결제조회 API 호출 실패 (승인/취소 응답 유실 시 실제 상태 대조용, 재시도 가능)
    PAYMENT_LOOKUP_FAILED(502, "결제 상태를 확인하지 못했습니다. 잠시 후 다시 시도해주세요"),
    // ===== 0906 문건우 수정 끝 =====

    // ========== 상품 (PRODUCT) ==========
    //[추가]
    PRODUCT_NOT_FOUND(404, "존재하지 않는 상품입니다"),
    PRODUCT_INVALID_FILE_FORMAT(400, "PNG, JPG 파일만 업로드 가능합니다"),
    PRODUCT_FILE_SIZE_EXCEEDED(400, "파일 크기는 5MB 이하여야 합니다"),
    PRODUCT_INVALID_DEADLINE(400, "마감일은 오늘 이후여야 합니다"),
    PRODUCT_INVALID_CATEGORY(400, "올바른 학과 분류를 선택해주세요"),
    PRODUCT_BARCODE_API_ERROR(502, "도서 정보를 불러오지 못했습니다. 직접 입력해주세요"),
    PRODUCT_SEARCH_UPSTREAM_ERROR(502, "상품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    PRODUCT_DUPLICATE(409, "이미 등록된 상품입니다"),
    PRODUCT_REGISTER_FAILED(500, "상품 등록에 실패했습니다. 잠시 후 다시 시도해주세요"),
    PRODUCT_DELETE_FAILED(500, "삭제에 실패했습니다. 잠시 후 다시 시도해주세요"),
    PRODUCT_UPDATE_FAILED(500, "수정에 실패했습니다. 잠시 후 다시 시도해주세요"),
    PRODUCT_CANNOT_DELETE_WITH_PARTICIPANTS(409, "이미 참여자가 있는 프로젝트는 삭제할 수 없습니다"),
    PRODUCT_CANNOT_MODIFY_COMPLETED(409, "목표 달성 완료된 프로젝트는 수정하거나 삭제할 수 없습니다"),
  
    // ===== 0906 문건우 수정 =====
    // PRD-RQ-004/005: 가격·목표인원 수정 정책 및 숫자 입력 검증
    // [수정][fix/exception-nfr] NFR-001: 요청값을 기존 저장된 가격(상태)과 비교해 거부하는
    // 리소스 상태 충돌이므로 400→409로 재분류 (형제 코드 PRODUCT_PRICE_CHANGE_HAS_PARTICIPANTS와 동일 패턴)
    PRODUCT_PRICE_INCREASE_NOT_ALLOWED(409, "가격은 인상할 수 없습니다"),
    PRODUCT_PRICE_CHANGE_HAS_PARTICIPANTS(409, "이미 결제가 완료된 참여자가 있어 가격을 수정할 수 없습니다"),
    // [수정][fix/exception-nfr] NFR-001: 요청값을 현재 참여 인원(상태)과 비교해 거부하는
    // 리소스 상태 충돌이므로 400→409로 재분류
    PRODUCT_TARGET_COUNT_BELOW_CURRENT(409, "목표 인원은 현재 참여 인원보다 적게 설정할 수 없습니다"),
    PRODUCT_PRICE_INVALID_UNIT(400, "가격은 100원 단위로 입력해주세요"),
    PRODUCT_SELLER_WITHDRAWN(409, "판매자가 탈퇴하여 더 이상 참여할 수 없는 상품입니다"),
    PRODUCT_INTEGRITY_TAMPERED(409, "상품 데이터 무결성 검증에 실패하여 공동구매에 참여할 수 없습니다"),

    // ========== 도서 검색 (BOOK SEARCH) ==========
    BOOK_SEARCH_INVALID_QUERY(400, "검색어를 입력해 주세요."),
    BOOK_SEARCH_NOT_CONFIGURED(503, "도서 검색 서비스가 설정되지 않았습니다. 관리자에게 문의해 주세요."),
    BOOK_SEARCH_AUTH_ERROR(502, "도서 검색 서비스 인증에 실패했습니다. 관리자에게 문의해 주세요."),
    BOOK_SEARCH_QUOTA_EXCEEDED(429, "도서 검색 요청이 많아 일시적으로 제한되었습니다. 잠시 후 다시 시도해 주세요."),
    BOOK_SEARCH_UPSTREAM_ERROR(502, "도서 검색 서비스에 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
    BOOK_SEARCH_TIMEOUT(504, "도서 검색 서비스 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."),

    // ========== 판매자 (SELLER) ==========
    SELLER_STATISTICS_LOAD_FAILED(500, "통계 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    SELLER_SALES_LIST_LOAD_FAILED(500, "판매 현황을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    SELLER_NOTIFICATION_LOAD_FAILED(500, "알림을 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    NOTIFICATION_NOT_FOUND(404, "존재하지 않는 알림입니다"),
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
    // [수정][fix/exception-nfr] NFR-001: SettlementService에서 findById().orElseThrow()로 던져지는
    // "존재하지 않는 출금 요청" 케이스였음 — 서버 내부 오류가 아니라 리소스 없음이므로 500→404로 재분류
    ADMIN_TRANSACTION_APPROVE_FAILED(404, "존재하지 않는 출금 요청입니다"),
    ADMIN_TRANSACTION_REJECT_FAILED(404, "존재하지 않는 출금 요청입니다"),
    ADMIN_HISTORY_LOAD_FAILED(500, "히스토리를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_ANOMALY_DETAIL_LOAD_FAILED(500, "이상 감지 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),
    ADMIN_SYNC_LOG_LOAD_FAILED(500, "동기화 로그를 불러오지 못했습니다. 잠시 후 다시 시도해주세요"),

    ADMIN_SIMULATION_NOT_FOUND(404, "보안 시뮬레이션을 찾을 수 없습니다"),
    ADMIN_SIMULATION_CONFLICT(409, "해당 상품에 진행 중인 시뮬레이션 또는 정상 변경이 있습니다"),
    ADMIN_SIMULATION_NOT_ELIGIBLE(409, "현재 시뮬레이션할 수 없는 상품입니다"),
    ADMIN_SIMULATION_SNAPSHOT_NOT_FOUND(409, "복구할 시뮬레이션 스냅샷이 없습니다"),
    ADMIN_SIMULATION_BLOCKCHAIN_UNAVAILABLE(503, "블록체인 원본을 확인할 수 없습니다"),

    // ========== 채팅 (CHAT) ==========
    // WebSocket 에러(WS_CONNECT_FAILED, WS_DISCONNECTED, WS_SEND_FAILED)는 STOMP 프레임으로 처리
    // → HTTP 응답이 아니므로 이 enum에 포함하지 않음 (SharedChatPage WebSocket 핸들러 참고)
    CHAT_ROOM_NOT_FOUND(404, "채팅방을 찾을 수 없습니다"),
    CHAT_ACCESS_DENIED(403, "접근 권한이 없는 채팅방입니다"),
    CHAT_HISTORY_LOAD_FAILED(500, "채팅 내역을 불러오지 못했습니다"),
    // [CHAT-RQ-002][feature/chat-fixes] 메시지 전송 취소
    CHAT_MESSAGE_NOT_FOUND(404, "존재하지 않는 메시지입니다"),
    CHAT_MESSAGE_NOT_OWNER(403, "본인이 보낸 메시지만 취소할 수 있습니다"),
    CHAT_INVALID_FILE_FORMAT(400, "이미지 파일(PNG/JPG/GIF/WEBP)만 업로드 가능합니다"),
    CHAT_FILE_SIZE_EXCEEDED(400, "파일 크기는 5MB 이하여야 합니다"),
    // [CHAT-RQ-002][fix/chat] 이미지 업로드 실패 시 내부 예외 메시지 노출 방지 — 고정 메시지로 대체, 원인은 서버 로그로만 확인
    CHAT_IMAGE_UPLOAD_FAILED(500, "이미지 업로드에 실패했습니다. 잠시 후 다시 시도해주세요"),
    // [CHAT-RQ-002][fix/chat] 클라이언트가 JOIN/LEAVE 등 시스템 전용 MessageType을 위조해 보내는 것 차단
    CHAT_INVALID_MESSAGE_TYPE(400, "허용되지 않는 메시지 타입입니다"),
    // [CHAT-RQ-001][fix/chat] 나간 사용자가 메시지 전송/취소를 시도 — 발신자 본인 나가기 상태 검증
    CHAT_ROOM_LEFT(403, "채팅방을 나가서 더 이상 이용할 수 없습니다"),
    // [CHAT-RQ-001][fix/chat] 탈퇴한 상대에게는 메시지를 보낼 수 없음 (프론트 입력창 비활성화 우회 방지용 서버 검증)
    CHAT_RECIPIENT_WITHDRAWN(403, "탈퇴한 상대에게는 메시지를 보낼 수 없습니다"),
    // [CHAT-RQ-003][fix/chat] 채팅방 생성 요청의 sellerEmail이 실제 상품의 판매자와 일치하지 않음
    CHAT_ROOM_SELLER_MISMATCH(400, "요청한 판매자 정보가 상품 정보와 일치하지 않습니다"),

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
    // [수정][fix/exception-nfr] NFR-001: SettlementService에서 findBySellerEmail().orElseThrow()로
    // 던져지는 "정산 계좌 없음" 케이스이므로 400→404로 재분류
    MYPAGE_SETTLEMENT_NO_ACCOUNT(404, "출금을 위한 정산 계좌가 등록되어 있지 않습니다"),
    MYPAGE_SETTLEMENT_WITHDRAW_FAILED(500, "출금 신청에 실패했습니다. 잠시 후 다시 시도해주세요"),
    MYPAGE_SETTLEMENT_ACCOUNT_UPDATE_FAILED(500, "계좌 변경에 실패했습니다. 잠시 후 다시 시도해주세요"),

    // ========== 거래 후기 (REVIEW) ==========
    REVIEW_NOT_ELIGIBLE(403, "정상 종료되고 결제 완료된 공동구매만 후기를 작성할 수 있습니다"),
    REVIEW_ALREADY_WRITTEN(409, "이미 이 공동구매에 후기를 작성했습니다"),
    REVIEW_NOT_OWNER(403, "본인이 작성한 후기만 수정하거나 삭제할 수 있습니다"),
    REVIEW_NOT_FOUND(404, "존재하지 않는 후기입니다");

    private final int status;
    private final String message;

    ErrorCode(final int status, final String message) {
        this.status = status;
        this.message = message;
    }
}
