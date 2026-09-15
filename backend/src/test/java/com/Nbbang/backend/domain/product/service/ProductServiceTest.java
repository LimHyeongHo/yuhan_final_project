package com.Nbbang.backend.domain.product.service;

import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.payment.repository.PaymentRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.entity.ProductPriceHistory;
import com.Nbbang.backend.domain.product.repository.ParticipationRepository;
import com.Nbbang.backend.domain.product.repository.ProductPriceHistoryRepository;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.product.repository.ScrapRepository;
import com.Nbbang.backend.domain.search.service.KakaoBookSearchService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private ProductHashService productHashService;
    private BlockchainService blockchainService;
    private PaymentRepository paymentRepository;
    private ProductPriceHistoryRepository productPriceHistoryRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productHashService = mock(ProductHashService.class);
        blockchainService = mock(BlockchainService.class);
        paymentRepository = mock(PaymentRepository.class);
        productPriceHistoryRepository = mock(ProductPriceHistoryRepository.class);
        productService = new ProductService(
                productRepository,
                mock(ParticipationRepository.class),
                mock(ScrapRepository.class),
                mock(UserAccountRepository.class),
                productHashService,
                mock(KakaoBookSearchService.class),
                blockchainService,
                mock(VerificationService.class),
                paymentRepository,
                productPriceHistoryRepository);
    }

    @Test
    void bookUpdateChangesCategoryAndPurchaseConditions() {
        Product existing = existingBook();
        Product requested = manipulatedBookUpdate(new BigDecimal("18000"));
        MultipartFile image = mock(MultipartFile.class);

        when(productRepository.findById(92L)).thenReturn(Optional.of(existing));
        when(paymentRepository.existsByProductIdAndStatus(92L, "DONE")).thenReturn(false);
        when(productHashService.calculateHash(existing)).thenReturn("new-data-hash");

        Product result = productService.updateProduct(92L, requested, image, "seller@yuhan.ac.kr");

        assertThat(result).isSameAs(existing);
        assertThat(existing.getPrice()).isEqualByComparingTo("18000");
        assertThat(existing.getTargetCount()).isEqualTo(12);
        assertThat(existing.getDescription()).isEqualTo("변경된 설명");
        assertThat(existing.getCategory()).isEqualTo("ARTIFICIAL_INTELLIGENCE");
        assertBookMetadataWasPreserved(existing);
        verifyNoInteractions(image);

        verify(productHashService).calculateHash(existing);
        verify(blockchainService).recordHashAsync(92L, "new-data-hash");

        ArgumentCaptor<ProductPriceHistory> historyCaptor = ArgumentCaptor.forClass(ProductPriceHistory.class);
        verify(productPriceHistoryRepository).save(historyCaptor.capture());
        ProductPriceHistory history = historyCaptor.getValue();
        assertThat(history.getOldPrice()).isEqualByComparingTo("20000");
        assertThat(history.getNewPrice()).isEqualByComparingTo("18000");
        assertThat(history.getVersionNumber()).isEqualTo(2);
        assertThat(history.getNewDataHash()).isEqualTo("new-data-hash");
    }

    @Test
    void bookMetadataManipulationDoesNotTriggerBlockchainUpdate() {
        Product existing = existingBook();
        Product requested = manipulatedBookUpdate(new BigDecimal("20000"));

        when(productRepository.findById(92L)).thenReturn(Optional.of(existing));

        productService.updateProduct(92L, requested, null, "seller@yuhan.ac.kr");

        assertThat(existing.getCategory()).isEqualTo("ARTIFICIAL_INTELLIGENCE");
        assertBookMetadataWasPreserved(existing);
        verify(productHashService, never()).calculateHash(existing);
        verifyNoInteractions(blockchainService);
        verify(productPriceHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createDefaultsMissingCategoryToGeneral() {
        Product product = new Product();
        product.setPrice(new BigDecimal("20000"));
        when(productRepository.save(product)).thenReturn(product);
        when(productHashService.calculateHash(product)).thenReturn("data-hash");

        productService.createProduct(product, null);

        assertThat(product.getCategory()).isEqualTo("GENERAL");
    }

    @Test
    void createRejectsUnsupportedCategory() {
        Product product = new Product();
        product.setPrice(new BigDecimal("20000"));
        product.setCategory("BOOKS");

        assertThatThrownBy(() -> productService.createProduct(product, null))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_INVALID_CATEGORY));
        verifyNoInteractions(productRepository);
    }

    private Product existingBook() {
        Product product = new Product();
        product.setProductId(92L);
        product.setSellerEmail("seller@yuhan.ac.kr");
        product.setType("BOOK");
        product.setTitle("기존 도서명");
        product.setAuthor("기존 저자");
        product.setPublisher("기존 출판사");
        product.setCategory("COMPUTER_SOFTWARE");
        product.setIsbn("9781234567890");
        product.setOriginalPrice(new BigDecimal("25000"));
        product.setImageUrl("https://example.com/original-cover.jpg");
        product.setPrice(new BigDecimal("20000"));
        product.setTargetCount(10);
        product.setCurrentCount(2);
        product.setPriceVersion(1);
        product.setDescription("기존 설명");
        product.setDeadline(LocalDateTime.of(2026, 9, 30, 23, 59));
        product.setStatus("OPEN");
        return product;
    }

    private Product manipulatedBookUpdate(BigDecimal price) {
        Product requested = new Product();
        requested.setType("ITEM");
        requested.setTitle("조작된 제목");
        requested.setAuthor("조작된 저자");
        requested.setPublisher("조작된 출판사");
        requested.setCategory("ARTIFICIAL_INTELLIGENCE");
        requested.setIsbn("9780000000000");
        requested.setOriginalPrice(new BigDecimal("99900"));
        requested.setImageUrl("https://example.com/manipulated-cover.jpg");
        requested.setPrice(price);
        requested.setTargetCount(12);
        requested.setDescription("변경된 설명");
        requested.setDeadline(LocalDateTime.of(2027, 1, 1, 0, 0));
        return requested;
    }

    private void assertBookMetadataWasPreserved(Product product) {
        assertThat(product.getType()).isEqualTo("BOOK");
        assertThat(product.getTitle()).isEqualTo("기존 도서명");
        assertThat(product.getAuthor()).isEqualTo("기존 저자");
        assertThat(product.getPublisher()).isEqualTo("기존 출판사");
        assertThat(product.getIsbn()).isEqualTo("9781234567890");
        assertThat(product.getOriginalPrice()).isEqualByComparingTo("25000");
        assertThat(product.getImageUrl()).isEqualTo("https://example.com/original-cover.jpg");
        assertThat(product.getDeadline()).isEqualTo(LocalDateTime.of(2026, 9, 30, 23, 59));
    }
}
