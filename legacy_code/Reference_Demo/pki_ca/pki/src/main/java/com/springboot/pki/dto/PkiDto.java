package com.springboot.pki.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PkiDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class SignupRequest {
        private String userId;
        private String ci; // Raw CI ( Connection Information)
        private String password;
        private String publicKey; // Base64
        private String deviceId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ChallengeRequest {
        private String userId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ChallengeResponse {
        private String challenge; // Nonce + Timestamp
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        private String userId;
        private String password;
        private String signature; // Signed challenge
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PurchaseRequest {
        private String userId;
        private String itemInfo;
        private int price;
        private String timestamp;
        private String signature; // Signed [itemInfo + price + timestamp]
    }
}
