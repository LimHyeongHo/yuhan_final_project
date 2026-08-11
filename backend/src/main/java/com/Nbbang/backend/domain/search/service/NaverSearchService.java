package com.Nbbang.backend.domain.search.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class NaverSearchService {

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    public List<Map<String, String>> search(String query) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://openapi.naver.com/v1/search/shop.json?query={query}&display=10";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class, query);
            List<Map<String, String>> results = new ArrayList<>();
            
            if (response.getBody() != null && response.getBody().containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
                for (Map<String, Object> item : items) {
                    Map<String, String> map = new HashMap<>();
                    // HTML 태그 제거
                    map.put("title", item.get("title").toString().replaceAll("<[^>]*>", ""));
                    map.put("mallName", item.get("mallName") != null ? item.get("mallName").toString() : "");
                    map.put("maker", item.get("maker") != null ? item.get("maker").toString() : "");
                    map.put("brand", item.get("brand") != null ? item.get("brand").toString() : "");
                    map.put("category", item.get("category1") != null ? item.get("category1").toString() : "");
                    map.put("image", item.get("image") != null ? item.get("image").toString() : "");
                    map.put("price", item.get("lprice") != null ? item.get("lprice").toString() : "");
                    map.put("description", ""); // 네이버 쇼핑 OpenAPI는 상세 설명을 제공하지 않음
                    results.add(map);
                }
            }
            return results;
        } catch (Exception e) {
            System.err.println("Naver API Error: " + e.getMessage());
            e.printStackTrace();
            List<Map<String, String>> errorResults = new ArrayList<>();
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("error", "Naver API 연동 오류: " + e.getMessage());
            errorResults.add(errorResult);
            return errorResults;
        }
    }
}
