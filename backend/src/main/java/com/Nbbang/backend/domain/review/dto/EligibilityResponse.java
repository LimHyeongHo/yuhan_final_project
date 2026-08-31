package com.Nbbang.backend.domain.review.dto;

import lombok.Getter;

/**
 * 특정 공동구매에 대한 후기 작성 자격 응답.
 * 프론트가 이 응답 하나로 버튼 상태를 결정한다:
 *  - myReview != null  → 이미 작성함 → "수정/삭제" UI
 *  - eligible == true   → "후기 작성" 버튼 노출
 *  - 둘 다 아님          → 버튼 숨김 (reason 은 안내 문구)
 */
@Getter
public class EligibilityResponse {

    private final boolean eligible;
    private final String reason;
    private final ReviewResponse myReview;

    private EligibilityResponse(boolean eligible, String reason, ReviewResponse myReview) {
        this.eligible = eligible;
        this.reason = reason;
        this.myReview = myReview;
    }

    public static EligibilityResponse eligible() {
        return new EligibilityResponse(true, null, null);
    }

    public static EligibilityResponse ineligible(String reason) {
        return new EligibilityResponse(false, reason, null);
    }

    public static EligibilityResponse alreadyWritten(ReviewResponse myReview) {
        return new EligibilityResponse(false, "이미 이 공동구매에 후기를 작성했습니다", myReview);
    }
}
