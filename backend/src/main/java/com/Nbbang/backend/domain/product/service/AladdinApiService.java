package com.Nbbang.backend.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AladdinApiService {

    @Value("${aladin.ttb.key}")
    private String ttbKey;

    @Value("${aladin.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 알라딘 API를 호출하여 ISBN에 해당하는 도서의 공식 정가를 반환합니다.
     * @param isbn 10자리 또는 13자리 ISBN
     * @return 알라딘 정가 (실패 시 null 반환)
     */
    public BigDecimal fetchAladdinPrice(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return null;
        }

        try {
            // 알라딘 API 요청 URL 구성 (JSON 형식으로 요청)
            String requestUrl = String.format("%s?ttbkey=%s&itemIdType=ISBN13&ItemId=%s&output=js&Version=20131101",
                    apiUrl, ttbKey, isbn);

            ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 알라딘 API는 마지막에 ; 가 붙어오는 경우가 있으므로 제거
                String jsonBody = response.getBody().replaceAll(";$","");
                JSONObject jsonObject = JSON.parseObject(jsonBody);

                JSONArray items = jsonObject.getJSONArray("item");
                if (items != null && !items.isEmpty()) {
                    JSONObject item = items.getJSONObject(0);
                    Integer priceStandard = item.getInteger("priceStandard");
                    if (priceStandard != null) {
                        return new BigDecimal(priceStandard);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("알라딘 API 통신 오류: " + e.getMessage());
        }

        return null; // 조회 실패 또는 정가 정보 없음
    }
}
