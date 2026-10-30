package com.Nbbang.backend.domain.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

// 채팅 이미지를 Supabase Storage에 저장 (배포 환경에서 이미지 깨짐 방지)
@Service
public class ChatImageStorageService {

    private static final String BUCKET = "chat-images";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String supabaseUrl;
    private final String serviceKey;

    public ChatImageStorageService(@Value("${supabase.url:}") String supabaseUrl,
                                   @Value("${supabase.service-key:}") String serviceKey) {
        this.supabaseUrl = supabaseUrl.trim().replaceAll("/+$", "");
        this.serviceKey = serviceKey.trim();
    }

    public String upload(String filename, byte[] bytes, String contentType) {
        if (supabaseUrl.isEmpty() || serviceKey.isEmpty()) {
            throw new IllegalStateException("SUPABASE_URL, SUPABASE_SERVICE_KEY 설정 필요");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceKey);
        // 키(JWT)를 Authorization 헤더에 넣음(새 형식 sb_secret 키는 apikey만 사용)
        if (serviceKey.startsWith("eyJ")) {
            headers.setBearerAuth(serviceKey);
        }
        headers.setContentType(MediaType.parseMediaType(contentType));

        String objectPath = BUCKET + "/" + filename;
        restTemplate.postForEntity(supabaseUrl + "/storage/v1/object/" + objectPath,
                new HttpEntity<>(bytes, headers), String.class);
        return supabaseUrl + "/storage/v1/object/public/" + objectPath;
    }
}
