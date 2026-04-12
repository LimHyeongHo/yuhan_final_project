package com.example.aisearchtrend.service;

import com.example.aisearchtrend.dto.SearchLog;
import com.example.aisearchtrend.dto.TrendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SearchTrendService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    // 검색어 저장 (메모리)
    // key: 검색어, value: 검색 횟수
    private final Map<String, Integer> searchCountMap = new ConcurrentHashMap<>();

    // 검색어 저장
    public void saveSearch(String keyword) {
        searchCountMap.merge(keyword, 1, Integer::sum);
        // merge: 키가 있으면 기존값 + 1, 없으면 1로 저장
    }

    // 검색 횟수 상위 10개 가져오기
    public List<SearchLog> getTopSearches() {
        return searchCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()) // 내림차순 정렬
                .limit(10) // 상위 10개
                .map(entry -> {
                    SearchLog log = new SearchLog();
                    log.setKeyword(entry.getKey());
                    log.setSearchCount(entry.getValue());
                    log.setLastSearched(LocalDateTime.now());
                    return log;
                })
                .collect(Collectors.toList());
    }

    public TrendResponse analyzeTrend() {

        List<SearchLog> topSearches = getTopSearches();

        if (topSearches.isEmpty()) {
            TrendResponse response = new TrendResponse();
            response.setTrendingBooks(new ArrayList<>());
            response.setRecommendedBooks(new ArrayList<>());
            response.setAnalysis("검색 데이터가 없습니다.");
            response.setGeneratedAt(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return response;
        }

        String searchData = topSearches.stream()
                .map(log -> log.getKeyword() + "(" + log.getSearchCount() + "회)")
                .collect(Collectors.joining(", "));

        // 프롬프트 수정 - 추천 도서까지 포함
        String prompt = "다음은 도서 공동구매 사이트의 최근 검색 데이터입니다.\n" +
                "검색어(검색횟수): " + searchData + "\n\n" +
                "위 데이터를 분석해서 아래 형식으로만 답해주세요. 다른 말은 하지 마세요.\n\n" +
                "인기급상승:\n" +
                "1위: 키워드\n" +
                "2위: 키워드\n" +
                "3위: 키워드\n\n" +
                "추천도서:\n" +
                "- 도서명 (이유 한 줄)\n" +
                "- 도서명 (이유 한 줄)\n" +
                "- 도서명 (이유 한 줄)\n\n" +
                "트렌드요약: 한 줄 요약";

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com")
                .build();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 500);
        requestBody.put("temperature", 0.7);

        Map response = webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String content = "";
        try {
            List<Map> choices = (List<Map>) response.get("choices");
            Map firstChoice = choices.get(0);
            Map messageMap = (Map) firstChoice.get("message");
            content = (String) messageMap.get("content");
        } catch (Exception e) {
            content = "분석 실패";
        }

        return parseResponse(content);
    }

    // 응답 파싱 메서드
    private TrendResponse parseResponse(String content) {
        TrendResponse trendResponse = new TrendResponse();
        List<String> trendingBooks = new ArrayList<>();
        List<String> recommendedBooks = new ArrayList<>();
        String analysis = "";

        try {
            String[] lines = content.split("\n");
            String section = "";

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("인기급상승:")) {
                    section = "trending";
                } else if (line.startsWith("추천도서:")) {
                    section = "recommend";
                } else if (line.startsWith("트렌드요약:")) {
                    analysis = line.replace("트렌드요약:", "").trim();
                } else if (section.equals("trending") && line.matches(".*위:.*")) {
                    trendingBooks.add(line.replaceAll("\\d+위:", "").trim());
                } else if (section.equals("recommend") && line.startsWith("-")) {
                    recommendedBooks.add(line.replace("-", "").trim());
                }
            }
        } catch (Exception e) {
            analysis = "파싱 오류";
        }

        trendResponse.setTrendingBooks(trendingBooks);
        trendResponse.setRecommendedBooks(recommendedBooks);
        trendResponse.setAnalysis(analysis);
        trendResponse.setGeneratedAt(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return trendResponse;
    }

    // 전체 검색 현황 조회
    public Map<String, Integer> getAllSearchCounts() {
        return searchCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }
}