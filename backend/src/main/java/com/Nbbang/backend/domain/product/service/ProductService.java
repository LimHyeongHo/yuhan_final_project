package com.Nbbang.backend.domain.product.service; // 🚨 본인 경로에 맞게 수정

import com.Nbbang.backend.domain.auth.entity.UserAccount;
import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.product.entity.Participation;
import com.Nbbang.backend.domain.product.entity.Scrap;
import com.Nbbang.backend.domain.product.repository.ParticipationRepository;
import com.Nbbang.backend.domain.product.repository.ScrapRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ParticipationRepository participationRepository;
    private final ScrapRepository scrapRepository;
    private final UserAccountRepository userAccountRepository;
    private final VerificationService verificationService;
    private final BlockchainService blockchainService;

    // 로컬 업로드 경로 설정 (프로젝트 실행 위치의 uploads 폴더)
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/";

    // 상품 등록 로직 (이미지 파일 로컬 저장 포함)
    @Transactional
    public Product createProduct(Product product, MultipartFile image) {
        if (product.getSellerId() == null) {
            product.setSellerId(1L); // 임시 유저 세팅
        }
        
        // 정가(originalPrice) 정보가 폼에 없어서 null일 경우 공구가와 동일하게 처리
        if (product.getOriginalPrice() == null) {
            product.setOriginalPrice(product.getPrice());
        }

        // 이미지 파일 처리
        if (image != null && !image.isEmpty()) {
            try {
                // uploads 폴더가 없으면 생성
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // 중복 방지를 위한 UUID 파일명 생성
                String originalFilename = image.getOriginalFilename();
                String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
                String savedFilename = UUID.randomUUID().toString() + extension;
                
                Path filePath = Paths.get(uploadDir + savedFilename);
                Files.write(filePath, image.getBytes());
                
                // 프론트엔드에서 접근할 수 있는 URL 경로 저장 (WebMvcConfigurer 연결 필요)
                product.setImageUrl("http://localhost:8080/uploads/" + savedFilename);
            } catch (IOException e) {
                e.printStackTrace();
                // 실제 서비스에서는 커스텀 예외 처리가 필요함
            }
        }

        Product savedProduct = productRepository.save(product);

        // [블록체인 연동] 상품 등록 시 비동기로 블록체인에 데이터 해시 기록
        String dataString = savedProduct.getProductId() + "_" + 
                           (savedProduct.getIsbn() != null ? savedProduct.getIsbn() : "") + "_" + 
                           (savedProduct.getPrice() != null ? savedProduct.getPrice().stripTrailingZeros().toPlainString() : "");
        String dataHash = verificationService.hashString(dataString);
        blockchainService.recordHashAsync(savedProduct.getProductId(), dataHash);

        return savedProduct;
    }

    // 전체 상품 조회 로직
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 개별 상품 상세 조회 로직
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
    }

    // 판매자 기준 상품 목록 조회
    public List<Product> getProductsBySellerId(Long sellerId) {
        return productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }

    // 판매자 기준 최근 참여자 내역 조회.
    // product는 지연 로딩(LAZY)이라, 트랜잭션(이 메서드) 밖인 컨트롤러에서 접근하면
    // open-in-view=false 설정 때문에 LazyInitializationException이 남 -> 여기서 미리 DTO로 변환해서 반환.
    public List<Map<String, Object>> getParticipationsBySellerId(Long sellerId) {
        return participationRepository.findByProduct_SellerIdOrderByJoinDateDesc(sellerId).stream()
                .map(part -> Map.<String, Object>of(
                        "id", part.getId(),
                        "buyerName", part.getBuyerName(),
                        "joinDate", part.getJoinDate(),
                        "product", Map.of("title", part.getProduct().getTitle())
                ))
                .toList();
    }

    // [신규] 로그인 이메일 기준 상품 목록 조회 (sellerId는 항상 1로 고정되는 임시값이라 실사용 불가)
    public List<Product> getProductsBySellerEmail(String sellerEmail) {
        return productRepository.findBySellerEmailOrderByCreatedAtDesc(sellerEmail);
    }

    // [신규] 로그인 이메일 기준 최근 참여자 내역 조회 (LazyInitializationException 방지 위해 서비스에서 DTO 변환)
    public List<Map<String, Object>> getParticipationsBySellerEmail(String sellerEmail) {
        return participationRepository.findByProduct_SellerEmailOrderByJoinDateDesc(sellerEmail).stream()
                .map(part -> Map.<String, Object>of(
                        "id", part.getId(),
                        "buyerName", part.getBuyerName(),
                        "joinDate", part.getJoinDate(),
                        "product", Map.of(
                                "title", part.getProduct().getTitle(),
                                "price", part.getProduct().getPrice()
                        )
                ))
                .toList();
    }

    // 공동구매 참여
    @Transactional
    public Product joinProduct(Long id, String userId) {
        Product product = getProductById(id);

        // 정원 초과 여부 확인
        if (product.getCurrentCount() != null && product.getCurrentCount() >= product.getTargetCount()) {
            throw new IllegalStateException("이미 목표 인원이 달성되었습니다.");
        }

        // 같은 사용자가 같은 공동구매에 중복 참여(=인원 조작)하는 것 방지
        if (participationRepository.existsByProduct_ProductIdAndMember_Email(id, userId)) {
            throw new CustomException(ErrorCode.PURCHASE_ALREADY_JOINED);
        }

        UserAccount member = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));

        product.incrementCurrentCount();

        Participation participation = new Participation();
        participation.setProduct(product);
        participation.setMember(member);
        participation.setBuyerName(member.getNickname());
        participationRepository.save(participation);

        return product; // 트랜잭션 종료 시 자동 더티 체킹으로 DB에 반영됨
    }

    // 공동구매 취소
    @Transactional
    public Product cancelJoinProduct(Long id) {
        Product product = getProductById(id);
        product.decrementCurrentCount();
        return product;
    }

    // 상품 수정 로직
    @Transactional
    public Product updateProduct(Long id, Product updatedData, MultipartFile image) {
        Product product = getProductById(id);
        
        // 필드 업데이트
        product.setTitle(updatedData.getTitle());
        product.setType(updatedData.getType());
        // product.setCategory(updatedData.getCategory()); // TODO: fix/search_bug 브랜치 병합 후 주석 해제 (category 연동)
        product.setPrice(updatedData.getPrice());
        product.setOriginalPrice(updatedData.getOriginalPrice() != null ? updatedData.getOriginalPrice() : updatedData.getPrice());
        product.setTargetCount(updatedData.getTargetCount());
        if (updatedData.getDeadline() != null) {
            product.setDeadline(updatedData.getDeadline());
        }
        product.setDescription(updatedData.getDescription());
        product.setPublisher(updatedData.getPublisher());
        product.setAuthor(updatedData.getAuthor());
        if (updatedData.getImageUrl() != null && !updatedData.getImageUrl().isEmpty()) {
            product.setImageUrl(updatedData.getImageUrl());
        }

        // 새 이미지가 있는 경우 업데이트
        if (image != null && !image.isEmpty()) {
            try {
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                String originalFilename = image.getOriginalFilename();
                String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
                String savedFilename = UUID.randomUUID().toString() + extension;
                
                Path filePath = Paths.get(uploadDir + savedFilename);
                Files.write(filePath, image.getBytes());
                
                product.setImageUrl("http://localhost:8080/uploads/" + savedFilename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return product;
    }

    // 상품 삭제 로직
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product); // CascadeType.ALL 이므로 참여내역도 자동 삭제됨
    }

    // [신규] DB 해킹 시뮬레이션 (블록체인 기록 없이 가격을 999,999원으로 강제 변경)
    @Transactional
    public void simulateDatabaseHack(Long id) {
        Product product = getProductById(id);
        product.setPrice(new java.math.BigDecimal("999999"));
        productRepository.save(product);
    }

    // [신규] 구매자 마이페이지용 참여 내역 조회
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyParticipations(String email) {
        return participationRepository.findByMember_EmailOrderByJoinDateDesc(email).stream()
            .map(part -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", part.getId());
                map.put("joinDate", part.getJoinDate());
                map.put("buyerName", part.getBuyerName());
                
                Product p = part.getProduct();
                Map<String, Object> productMap = new java.util.HashMap<>();
                productMap.put("id", p.getProductId()); // 프론트에서 product.id 로 사용함
                productMap.put("title", p.getTitle());
                productMap.put("price", p.getPrice());
                productMap.put("status", p.getStatus());
                productMap.put("currentCount", p.getCurrentCount());
                productMap.put("targetCount", p.getTargetCount());
                
                map.put("product", productMap);
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    // [신규] 스크랩 토글
    @Transactional
    public boolean toggleScrap(Long productId, String email) {
        Product product = getProductById(productId);
        UserAccount user = userAccountRepository.findById(email)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));

        return scrapRepository.findByProduct_ProductIdAndMember_Email(productId, email)
                .map(scrap -> {
                    scrapRepository.delete(scrap);
                    return false; // 스크랩 취소됨
                })
                .orElseGet(() -> {
                    Scrap newScrap = new Scrap();
                    newScrap.setProduct(product);
                    newScrap.setMember(user);
                    scrapRepository.save(newScrap);
                    return true; // 스크랩 추가됨
                });
    }

    // [신규] 마이페이지 스크랩 목록 조회
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyScraps(String email) {
        return scrapRepository.findByMember_EmailOrderByCreatedAtDesc(email).stream()
            .map(scrap -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", scrap.getId());
                
                Product p = scrap.getProduct();
                Map<String, Object> productMap = new java.util.HashMap<>();
                productMap.put("productId", p.getProductId());
                productMap.put("title", p.getTitle());
                productMap.put("author", p.getAuthor());
                productMap.put("currentCount", p.getCurrentCount());
                productMap.put("targetCount", p.getTargetCount());
                
                map.put("product", productMap);
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    // [신규] 특정 상품 스크랩 여부 확인
    @Transactional(readOnly = true)
    public boolean checkScrapStatus(Long productId, String email) {
        return scrapRepository.existsByProduct_ProductIdAndMember_Email(productId, email);
    }

// [신규] 판매자 주문 관리(명단 확인) 용 내역 조회
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSellerOrders(String sellerEmail) {
        List<Participation> participations = participationRepository.findByProduct_SellerEmailOrderByJoinDateDesc(sellerEmail);
        return participations.stream().map(p -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("participationId", p.getId());
            map.put("buyerEmail", p.getMember().getEmail());
            map.put("buyerNickname", p.getBuyerName());
            map.put("joinDate", p.getJoinDate());
            
            Product product = p.getProduct();
            map.put("productId", product.getProductId());
            map.put("productTitle", product.getTitle());
            map.put("productPrice", product.getPrice());
            map.put("productStatus", product.getStatus());
            
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }
}