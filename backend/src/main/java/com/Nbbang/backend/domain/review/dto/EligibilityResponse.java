package com.Nbbang.backend.domain.review.dto;

import lombok.Getter;

// 후기 작성 자격 응답 (이 응답 하나로 화면 버튼을 정함)
// myReview가 있으면 수정·삭제, eligible이면 작성 버튼, 둘 다 아니면 숨김 (reason은 안내 문구)
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
