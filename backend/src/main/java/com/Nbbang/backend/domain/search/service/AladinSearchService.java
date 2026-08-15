package com.Nbbang.backend.domain.search.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Service
public class AladinSearchService {

    @Value("${aladin.ttb.key}")
    private String ttbKey;

    public List<Map<String, String>> searchBook(String query) {
        RestTemplate restTemplate = new RestTemplate();
        
        String url;
        boolean isBarcode = query.matches("^[0-9]{10}$") || query.matches("^[0-9]{13}$");
        
        if (isBarcode) {
            url = "https://www.aladin.co.kr/ttb/api/ItemLookUp.aspx?ttbkey={ttbkey}&ItemIdType=ISBN13&ItemId={query}&output=js&Version=20131101";
        } else {
            // 결과 10개까지 가져오도록 변경
            url = "https://www.aladin.co.kr/ttb/api/ItemSearch.aspx?ttbkey={ttbkey}&Query={query}&QueryType=Keyword&MaxResults=10&start=1&SearchTarget=Book&output=js&Version=20131101";
        }

        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, Map.class, ttbKey.trim(), query.trim());
            Map<String, Object> responseMap = responseEntity.getBody();
            
            List<Map<String, String>> results = new ArrayList<>();
            
            if (responseMap == null) {
                Map<String, String> errorResult = new HashMap<>();
                errorResult.put("error", "알라딘 API 응답이 비어있습니다.");
                results.add(errorResult);
                return results;
            }
            
            // "item" 배열이 포함되어 있는지 확인
            if (!responseMap.containsKey("item")) {
                Map<String, String> errorResult = new HashMap<>();
                errorResult.put("error", "알라딘 API 결과가 없거나 오류 메시지입니다.");
                results.add(errorResult);
                return results;
            }
            List<Map<String, Object>> items = (List<Map<String, Object>>) responseMap.get("item");
            
            List<String> allowedKeywords = Arrays.asList("대학교재", "전문서적", "컴퓨터", "모바일", "수험서", "자격증", "과학", "공학", "인문", "사회", "어학", "외국어");
            
            for (Map<String, Object> item : items) {
                String categoryName = item.get("categoryName") != null ? item.get("categoryName").toString() : "";
                
                // 화이트리스트 검사
                boolean isAllowed = allowedKeywords.stream().anyMatch(categoryName::contains);
                
                if (isAllowed) {
                    // 카테고리를 '>' 기호로 분리하여 2번째(인덱스 1) 항목만 추출
                    String[] categoryParts = categoryName.split(">");
                    String displayCategory = categoryParts.length > 1 ? categoryParts[1].trim() : categoryName.trim();

                    Map<String, String> book = new HashMap<>();
                    book.put("title", item.get("title") != null ? item.get("title").toString() : "");
                    book.put("category", displayCategory);
                    book.put("author", item.get("author") != null ? item.get("author").toString() : "");
                    book.put("maker", item.get("publisher") != null ? item.get("publisher").toString() : "");
                    book.put("brand", ""); 
                    book.put("image", item.get("cover") != null ? item.get("cover").toString() : "");
                    book.put("description", item.get("description") != null ? item.get("description").toString() : "");
                    book.put("price", item.get("priceStandard") != null ? item.get("priceStandard").toString() : "");
                    book.put("isbn", item.get("isbn13") != null ? item.get("isbn13").toString() : ""); // [추가] ISBN 정보 반환
                    results.add(book);
                }
            }
            
            // 필터링 결과 남은 책이 없는 경우
            if (results.isEmpty() && items != null && !items.isEmpty()) {
                Map<String, String> errorResult = new HashMap<>();
                errorResult.put("error", "검색된 결과 중 전공도서(수험서, 컴퓨터 등) 카테고리에 속한 도서가 없습니다. (만화, 소설 등 차단)");
                results.add(errorResult);
            }
            
            return results;
            
        } catch (Exception e) {
            System.err.println("Aladin API Error: " + e.getMessage());
            List<Map<String, String>> errorResults = new ArrayList<>();
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("error", "알라딘 API 연동 오류: " + e.getMessage());
            errorResults.add(errorResult);
            return errorResults;
        }
    }
}
