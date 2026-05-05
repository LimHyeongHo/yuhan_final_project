package com.demo.gp.service;

import com.demo.gp.dto.DeadlineResult;
import com.demo.gp.entity.*;
import com.demo.gp.repository.*;
import com.demo.gp.entity.AdminLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class GroupPurchaseService {

    private static final Logger adminLog = LoggerFactory.getLogger("AdminActionLogger");

    private final GroupPurchaseRepository gpRepo;
    private final ParticipationRepository partRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final HistoryRepository historyRepo;
    private final AdminLogRepository adminLogRepo;
    private final com.demo.gp.DataInitializer dataInitializer;

    public GroupPurchaseService(GroupPurchaseRepository gpRepo,
                                ParticipationRepository partRepo,
                                ProductRepository productRepo,
                                UserRepository userRepo,
                                HistoryRepository historyRepo,
                                AdminLogRepository adminLogRepo,
                                com.demo.gp.DataInitializer dataInitializer) {
        this.gpRepo          = gpRepo;
        this.partRepo        = partRepo;
        this.productRepo     = productRepo;
        this.userRepo        = userRepo;
        this.historyRepo     = historyRepo;
        this.adminLogRepo    = adminLogRepo;
        this.dataInitializer = dataInitializer;
    }

    // 결제 완료 여부 확인
    private boolean isPaid(String status) {
        return "완료".equals(status) || "추가결제대기".equals(status);
    }

    @Transactional(readOnly = true)
    public List<GroupPurchase> getAllPurchases() { return gpRepo.findAll(); }

    @Transactional(readOnly = true)
    public List<Participation> getParticipations(Integer gpId) {
        return partRepo.findByGpId(gpId);
    }

    @Transactional(readOnly = true)
    public GroupPurchase getGroupPurchase(Integer gpId) {
        return gpRepo.findById(gpId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공동구매: " + gpId));
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProduct(Integer productId) {
        return productRepo.findById(productId);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUser(Integer userId) {
        return userRepo.findById(userId);
    }

    // 결과 시뮬레이션용 (DB 저장 안 함)
    @Transactional(readOnly = true)
    public DeadlineResult processDeadline(Integer gpId) {
        return doDeadlineLogic(gpId, false, null);
    }

    // 마감 핵심 로직 (commit=true일 때만 DB 저장)
    private DeadlineResult doDeadlineLogic(Integer gpId, boolean commit, String overrideStatus) {
        GroupPurchase gp = gpRepo.findById(gpId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공동구매: " + gpId));

        Product product = productRepo.findById(gp.getProductId())
                .orElseThrow(() -> new IllegalStateException("상품 정보 없음: " + gp.getProductId()));

        List<Participation> allParts = partRepo.findByGpId(gpId);
        if (allParts.isEmpty()) throw new IllegalStateException("참여자가 없는 공동구매: " + gpId);

        Map<Integer, User> userMap = buildUserMap(allParts);

        DeadlineResult result = new DeadlineResult();
        result.setGpId(gpId);
        result.setTitle(product.getTitle());
        result.setBuyerMap(userMap);

        int originalQty = gp.getCurrentQty();
        int beforePrice = allParts.get(0).getFinalPrice();
        result.setBeforeQty(originalQty);
        result.setBeforePrice(beforePrice);

        List<Participation> failedParts = new ArrayList<>();
        List<String> failedUsers = handleUnpaidUsers(allParts, failedParts, userMap, commit);
        result.setFailedUsers(failedUsers);

        // 결제 완료 인원만 추출
        List<Participation> paidParts = allParts.stream()
                .filter(p -> isPaid(p.getPaymentStatus()))
                .collect(Collectors.toList());

        int newQty = paidParts.size();
        gp.setCurrentQty(newQty);
        result.setAfterQty(newQty);

        // 1인당 결제금액 재계산
        int newPrice = recalculatePrice(product.getBasePrice(), newQty);
        result.setAfterPrice(newPrice);
        result.setExtraPayment(newPrice - beforePrice);

        int minQty = gp.getMinQty();
        if (newQty < minQty) {
            result.setMinQtyFailed(true);
            result.setScenario("B - 예외 처리 (최소 인원 미달)");
            List<String> refunded = updateStatus(gp, paidParts, "실패", userMap);
            result.setRefundedUsers(refunded);
            String targetStatus = (overrideStatus != null) ? overrideStatus : "실패";
            gp.setStatus(targetStatus);
            result.setFinalStatus(targetStatus);
        } else {
            result.setMinQtyFailed(false);
            result.setScenario("A - 정상 처리");
            paidParts.forEach(p -> p.setFinalPrice(newPrice));
            String targetStatus = (overrideStatus != null) ? overrideStatus : "발주대기";
            gp.setStatus(targetStatus);
            result.setFinalStatus(targetStatus);
        }

        if (commit) {
            List<Participation> changedParts = new ArrayList<>(failedParts);
            changedParts.addAll(paidParts);
            gpRepo.save(gp);
            partRepo.saveAll(changedParts);
        }

        result.setAllParticipations(allParts);
        return result;
    }

    // 정상 발주 확정 처리
    public void confirmOrder(Integer gpId) {
        GroupPurchase gp = gpRepo.findById(gpId).orElseThrow();
        if ("발주진행".equals(gp.getStatus())) return;

        doDeadlineLogic(gpId, true, "발주진행");

        Product product = productRepo.findById(gp.getProductId()).orElseThrow();

        // 결제완료 데이터 수집
        List<Participation> paidParts = partRepo.findByGpId(gpId).stream()
                .filter(p -> isPaid(p.getPaymentStatus()))
                .collect(Collectors.toList());

        for (Participation p : paidParts) {
            History h = new History();
            h.setBuyerId(p.getBuyerId());
            h.setSellerId(product.getSellerId());
            h.setStatus("구매완료");
            h.setTitle(product.getTitle());
            h.setFinalPrice(p.getFinalPrice());
            h.setImageUrl(product.getImageUrl());
            h.setProductId(product.getProductId());
            h.setCreatedAt(LocalDateTime.now());
            historyRepo.save(h);
        }

        String scenarioLabel = (gpId == 2) ? "시나리오B" : "시나리오A";
        String msg = String.format("[%s] 발주 확정 - 결제완료 %d명, 발주진행으로 전환", scenarioLabel, paidParts.size());
        adminLog.info(msg);
        saveAdminLog(scenarioLabel, "발주확정", msg);
    }

    // 실패 및 환불 확정 처리
    public void confirmFail(Integer gpId) {
        GroupPurchase checkGp = gpRepo.findById(gpId).orElseThrow();
        if ("실패".equals(checkGp.getStatus())) return;
        doDeadlineLogic(gpId, true, "실패");

        String scenarioLabel = (gpId == 2) ? "시나리오B" : "시나리오A";
        String msg = String.format("[%s] 실패 확정 - 공동구매 실패 처리 및 환불 완료", scenarioLabel);
        adminLog.info(msg);
        saveAdminLog(scenarioLabel, "실패확정", msg);
    }

    // 단가 인상 후 마감 연장
    public void reopenRecruitment(Integer gpId) {
        GroupPurchase gp = gpRepo.findById(gpId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공동구매: " + gpId));

        List<Participation> allParts = partRepo.findByGpId(gpId);
        Map<Integer, User> userMap = buildUserMap(allParts);

        // 미입금자 퇴출 및 경고 횟수 증가
        List<Participation> failedParts = new ArrayList<>();
        List<User> usersToUpdate = new ArrayList<>();

        for (Participation p : allParts) {
            if ("대기".equals(p.getPaymentStatus())) {
                failedParts.add(p);
                User u = userMap.get(p.getBuyerId());
                if (u != null) {
                    u.setMissCount(u.getMissCount() + 1);
                    usersToUpdate.add(u);
                }
            }
        }

        if (!usersToUpdate.isEmpty()) userRepo.saveAll(usersToUpdate);
        if (!failedParts.isEmpty()) {
            partRepo.deleteAll(failedParts);
            partRepo.flush();
        }

        // 결제 완료된 남은 인원수 파악
        List<Participation> paidParts = allParts.stream()
                .filter(p -> "완료".equals(p.getPaymentStatus()))
                .collect(Collectors.toList());

        long activeCount = paidParts.size();
        gp.setCurrentQty((int) activeCount);

        // 단가 재계산 후 변동 시 추가결제 대기로 변경
        Product prod = productRepo.findById(gp.getProductId()).orElseThrow();
        if (activeCount > 0) {
            int newPrice = prod.getBasePrice() / (int) activeCount;
            int oldPrice = paidParts.get(0).getFinalPrice();
            if (newPrice != oldPrice) {
                paidParts.forEach(p -> {
                    p.setFinalPrice(newPrice);
                    p.setPaymentStatus("추가결제대기");
                });
                partRepo.saveAll(paidParts);
            }
        }
        // 참여자 수 갱신 및 기간 1일 연장
        gp.setCurrentQty(paidParts.size());
        gp.setEndAt(gp.getEndAt().plusDays(1));
        gp.setStatus("모집중");
        gpRepo.save(gp);

        String scenarioLabel = (gpId == 2) ? "시나리오B" : "시나리오A";
        String msg = String.format("[%s] 관리자 액션: 모집중(마감기간 연장) - 미입금자 %d명 퇴출 처리 완료", scenarioLabel, failedParts.size());
        adminLog.info(msg);
        saveAdminLog(scenarioLabel, "마감연장", msg);
    }

    // 시연용 초기화
    public void resetAllData() {
        partRepo.deleteAllInBatch();
        historyRepo.deleteAllInBatch();
        gpRepo.deleteAllInBatch();
        productRepo.deleteAllInBatch();
        userRepo.deleteAllInBatch();
        dataInitializer.init();
    }

    private Map<Integer, User> buildUserMap(List<Participation> parts) {
        Map<Integer, User> map = new HashMap<>();
        for (Participation p : parts) {
            userRepo.findById(p.getBuyerId())
                    .ifPresent(u -> map.put(p.getBuyerId(), u));
        }
        return map;
    }

    private List<String> handleUnpaidUsers(List<Participation> parts,
                                           List<Participation> failedParts,
                                           Map<Integer, User> userMap,
                                           boolean commit) {
        List<String> failedUsers = new ArrayList<>();
        for (Participation p : parts) {
            if ("대기".equals(p.getPaymentStatus())) {
                p.setPaymentStatus("실패");
                failedParts.add(p);
                User u = userMap.get(p.getBuyerId());
                if (u != null) {
                    if (commit) {
                        u.setMissCount(u.getMissCount() + 1);
                        userRepo.save(u);
                    }
                    failedUsers.add(u.getNickname());
                } else {
                    failedUsers.add("알수없음");
                }
            }
        }
        return failedUsers;
    }

    private void saveAdminLog(String scenario, String action, String message) {
        AdminLog log = new AdminLog();
        log.setCreatedAt(LocalDateTime.now());
        log.setScenario(scenario);
        log.setAction(action);
        log.setMessage(message);
        adminLogRepo.save(log);
    }

    // N빵 계산
    private int recalculatePrice(int basePrice, int newQty) {
        if (newQty == 0) return 0;
        return basePrice / newQty;
    }

    private List<String> updateStatus(GroupPurchase gp,
                                      List<Participation> paidParts,
                                      String newStatus,
                                      Map<Integer, User> userMap) {
        gp.setStatus(newStatus);
        List<String> refundedUsers = new ArrayList<>();
        if ("실패".equals(newStatus)) {
            for (Participation p : paidParts) {
                p.setPaymentStatus("환불");
                User u = userMap.get(p.getBuyerId());
                refundedUsers.add(u != null ? u.getNickname() : "알수없음");
            }
        }
        return refundedUsers;
    }
}