package com.Nbbang.backend.domain.payment.controller;

import com.Nbbang.backend.domain.payment.service.PaymentService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    private PaymentService paymentService;
    private PaymentController controller;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        controller = new PaymentController(paymentService);
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void redirectsWithAnAllowedErrorCodeInsteadOfTheExceptionMessage() throws Exception {
        when(paymentService.processSuccessCallback("order-1", "payment-key", 10_000L))
                .thenThrow(new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.paymentSuccess("payment-key", "order-1", 10_000L, "도서", response);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/payment/fail?code=PAYMENT_AMOUNT_MISMATCH")
                .doesNotContain("message=");
    }

    @Test
    void doesNotReflectAnUnknownProviderErrorCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.paymentFail("<script>alert(1)</script>", response);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/payment/fail?code=PAYMENT_CONFIRM_FAILED")
                .doesNotContain("script", "message=");
    }
}
