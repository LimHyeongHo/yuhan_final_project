package com.springboot.pki.controller;

import com.springboot.pki.dto.PkiDto.*;
import com.springboot.pki.entity.User;
import com.springboot.pki.repository.UserRepository;
import com.springboot.pki.service.PkiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/pki")
@RequiredArgsConstructor
public class PkiController {

    private final UserRepository userRepository;
    private final PkiService pkiService;

    // 1. 회원가입 (사람 + 기기 바인딩)
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        // CI Hash 생성
        String ciHash = pkiService.generateCiHash(request.getCi());

        // 중복 가입 체크 (1인 1계정 강제)
        if (userRepository.findByCiHash(ciHash).isPresent()) {
            return ResponseEntity.badRequest().body("이미 가입된 사용자입니다. (1인 1계정 정책)");
        }

        // 아이디 중복 체크
        if (userRepository.findByUserId(request.getUserId()).isPresent()) {
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setUserId(request.getUserId());
        user.setCiHash(ciHash);
        user.setPassword(request.getPassword()); // 암호화 생략 (실무에선 BCrypt 권장)
        user.setPublicKey(request.getPublicKey());
        user.setDeviceId(request.getDeviceId());
        
        userRepository.save(user);
        return ResponseEntity.ok("회원가입 성공 (CI Hash & 기기 바인딩 완료)");
    }

    // 2. 챌린지 요청 (로그인 시작)
    @PostMapping("/challenge")
    public ResponseEntity<ChallengeResponse> getChallenge(@RequestBody ChallengeRequest request) {
        Optional<User> user = userRepository.findByUserId(request.getUserId());
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String challenge = pkiService.createChallenge(request.getUserId());
        return ResponseEntity.ok(new ChallengeResponse(challenge));
    }

    // 3. 로그인 (지식 + 소유 기반 인증)
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        Optional<User> userOptional = userRepository.findByUserId(request.getUserId());
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();

        // 지식 기반 (비밀번호) 검증
        if (!user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401).body("비밀번호가 일치하지 않습니다.");
        }

        // 소유 기반 (PKI 챌린지-응답) 검증
        if (pkiService.validateChallenge(request.getUserId(), request.getSignature())) {
            return ResponseEntity.ok("로그인 성공 (비밀번호 + 전자서명 검증 완료)");
        } else {
            return ResponseEntity.status(401).body("전자서명 검증 실패 (본인 기기가 아닙니다)");
        }
    }

    // 4. 구매 검증 (거래 부인 방지)
    @PostMapping("/purchase")
    public ResponseEntity<String> purchase(@RequestBody PurchaseRequest request) {
        Optional<User> userOptional = userRepository.findByUserId(request.getUserId());
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();

        // [상품 정보 + 금액 + 타임스탬프] 서명 검증
        String rawData = request.getItemInfo() + request.getPrice() + request.getTimestamp();
        boolean isValid = pkiService.verifySignature(user.getPublicKey(), rawData, request.getSignature());

        if (isValid) {
            // 거래 로그 기록 (서명값 포함하여 부인 방지 증거 확보)
            System.out.println("거래 성공 및 증거 저장: " + request.getSignature());
            return ResponseEntity.ok("결제 성공 및 거래 부인 방지 증거 확보 완료");
        } else {
            return ResponseEntity.badRequest().body("거래 서명 검증 실패 (데이터 변조 또는 타인 서명)");
        }
    }
}
