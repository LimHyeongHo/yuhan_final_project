package com.demo.gp.controller;

import com.demo.gp.dto.DeadlineResult;
import com.demo.gp.entity.*;
import com.demo.gp.service.GroupPurchaseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class GroupPurchaseController {

    private final GroupPurchaseService service;

    public GroupPurchaseController(GroupPurchaseService service) {
        this.service = service;
    }

    // 메인 페이지 데이터 세팅
    @GetMapping("/")
    public String index(Model model) {
        List<GroupPurchase> list = service.getAllPurchases();

        // gpId → Product
        Map<Integer, Product> productMap = new LinkedHashMap<>();
        // sellerId → User (닉네임 표시용)
        Map<Integer, User> userMap = new LinkedHashMap<>();
        // gpId → 참여자 목록
        Map<Integer, List<Participation>> partMap = new LinkedHashMap<>();
        // buyerId → User (닉네임 표시용)
        Map<Integer, User> buyerMap = new LinkedHashMap<>();

        for (GroupPurchase gp : list) {
            // Product
            service.getProduct(gp.getProductId()).ifPresent(p -> {
                productMap.put(gp.getGpId(), p);
                // Seller
                service.getUser(p.getSellerId()).ifPresent(s -> userMap.put(p.getSellerId(), s));
            });

            // Participations
            List<Participation> parts = service.getParticipations(gp.getGpId());
            partMap.put(gp.getGpId(), parts);
            for (Participation p : parts) {
                service.getUser(p.getBuyerId()).ifPresent(u -> buyerMap.put(p.getBuyerId(), u));
            }
        }

        model.addAttribute("purchases",   list);
        model.addAttribute("productMap",  productMap);
        model.addAttribute("userMap",     userMap);
        model.addAttribute("partMap",     partMap);
        model.addAttribute("buyerMap",    buyerMap);
        return "index";
    }

    // 시연용 카운트다운 화면 진입
    @PostMapping("/scenario/{gpId}")
    public String scenario(@PathVariable Integer gpId, Model model) {
        GroupPurchase gp = service.getAllPurchases().stream()
                .filter(g -> g.getGpId().equals(gpId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid gpId"));

        List<Participation> parts = service.getParticipations(gpId);
        // 닉네임 표시용 매핑
        Map<Integer, User> buyerMap = new LinkedHashMap<>();
        for (Participation p : parts) {
            service.getUser(p.getBuyerId()).ifPresent(u -> buyerMap.put(p.getBuyerId(), u));
        }

        model.addAttribute("gp", gp);
        model.addAttribute("parts", parts);
        model.addAttribute("buyerMap", buyerMap);
        return "scenario";
    }

    // 마감 결과 시뮬레이션
    @PostMapping("/deadline/{gpId}")
    public String deadline(@PathVariable Integer gpId, Model model) {
        DeadlineResult result = service.processDeadline(gpId);
        model.addAttribute("result", result);
        return "result";
    }

    // 발주 확정 처리
    @PostMapping("/confirm/{gpId}/order")
    public String confirmOrder(@PathVariable Integer gpId, RedirectAttributes ra) {
        service.confirmOrder(gpId);
        ra.addFlashAttribute("flashMessage", "✅ 발주 확정 완료 — 발주진행으로 전환되었습니다.");
        ra.addFlashAttribute("flashType", "success");
        return "redirect:/";
    }

    // 실패 확정 및 환불
    @PostMapping("/confirm/{gpId}/fail")
    public String confirmFail(@PathVariable Integer gpId, RedirectAttributes ra) {
        service.confirmFail(gpId);
        ra.addFlashAttribute("flashMessage", "❌ 공동구매 실패 확정 — 결제자 환불 처리 완료.");
        ra.addFlashAttribute("flashType", "info");
        return "redirect:/";
    }

    // 마감 연장 및 모집 재개
    @PostMapping("/confirm/{gpId}/reopen")
    public String reopen(@PathVariable Integer gpId, RedirectAttributes ra) {
        service.reopenRecruitment(gpId);
        ra.addFlashAttribute("flashMessage", "🔄 추가 모집 재개 — 마감 시간이 하루 연장되었습니다.");
        ra.addFlashAttribute("flashType", "info");
        return "redirect:/";
    }

    // 초기 시연 데이터 리셋
    @PostMapping("/reset")
    public String reset() {
        service.resetAllData();
        return "redirect:/";
    }
}
