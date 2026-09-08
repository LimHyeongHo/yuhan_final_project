package com.Nbbang.backend.domain.admin.service;

import com.Nbbang.backend.domain.auth.repository.UserAccountRepository;
import com.Nbbang.backend.domain.notification.repository.NotificationRepository;
import com.Nbbang.backend.domain.product.entity.BlockchainJobStatus;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.product.service.BlockchainService;
import com.Nbbang.backend.domain.product.service.ProductHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private static final String DATA_HASH = "a".repeat(64);

    private ProductRepository productRepository;
    private BlockchainService blockchainService;
    private ProductHashService productHashService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        productRepository = mock(ProductRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        blockchainService = mock(BlockchainService.class);
        productHashService = mock(ProductHashService.class);
        adminService = new AdminService(
                userAccountRepository,
                productRepository,
                notificationRepository,
                blockchainService,
                productHashService);
    }

    @Test
    void alreadySyncedHashBackfillsConfirmedState() {
        Product product = product(1L);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productHashService.calculateHash(product)).thenReturn(DATA_HASH);
        when(blockchainService.readHash(1L))
                .thenReturn(BlockchainService.BlockchainReadResult.success(DATA_HASH));
        when(productHashService.matches(DATA_HASH, DATA_HASH)).thenReturn(true);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = adminService.migrateLegacyData();

        assertThat(product.getBlockchainStatus()).isEqualTo(BlockchainJobStatus.CONFIRMED);
        assertThat(product.getBlockchainRetryCount()).isZero();
        assertThat(product.getBlockchainLastError()).isNull();
        assertThat(product.getBlockchainUpdatedAt()).isNotNull();
        assertThat(result).containsEntry("alreadySyncedCount", 1);
        verify(productRepository).save(product);
    }

    @Test
    void notFoundHashStartsLegacyWriteInsteadOfBeingClassifiedAsRpcError() {
        Product product = product(2L);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productHashService.calculateHash(product)).thenReturn(DATA_HASH);
        when(blockchainService.readHash(2L))
                .thenReturn(BlockchainService.BlockchainReadResult.notFound("not found"));
        when(blockchainService.recordHashAndConfirm(2L, DATA_HASH))
                .thenReturn(BlockchainService.BlockchainWriteResult.success("0xtest"));

        Map<String, Object> result = adminService.migrateLegacyData();

        verify(blockchainService).recordHashAndConfirm(2L, DATA_HASH);
        assertThat(result).containsEntry("failedCount", 0);
        assertThat(result).containsEntry("status", "SUCCESS");
    }

    @Test
    void unavailableReadBackfillsRetryableFailure() {
        Product product = product(3L);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productHashService.calculateHash(product)).thenReturn(DATA_HASH);
        when(blockchainService.readHash(3L))
                .thenReturn(BlockchainService.BlockchainReadResult.unavailable("timeout"));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = adminService.migrateLegacyData();

        assertThat(product.getBlockchainStatus()).isEqualTo(BlockchainJobStatus.FAILED_RETRYABLE);
        assertThat(product.getBlockchainLastError()).isEqualTo("timeout");
        assertThat(result).containsEntry("failedCount", 1);
        verify(productRepository).save(product);
    }

    @Test
    void hashMismatchBackfillsFinalFailure() {
        Product product = product(4L);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productHashService.calculateHash(product)).thenReturn(DATA_HASH);
        when(blockchainService.readHash(4L))
                .thenReturn(BlockchainService.BlockchainReadResult.success("b".repeat(64)));
        when(productHashService.matches(DATA_HASH, "b".repeat(64))).thenReturn(false);
        when(productHashService.calculateLegacyMigrationHash(product)).thenReturn("legacy");
        when(productHashService.matches("legacy", "b".repeat(64))).thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = adminService.migrateLegacyData();

        assertThat(product.getBlockchainStatus()).isEqualTo(BlockchainJobStatus.FAILED_FINAL);
        assertThat(product.getBlockchainLastError()).isNotBlank();
        assertThat(result).containsEntry("mismatchCount", 1);
        verify(productRepository).save(product);
    }

    private Product product(Long productId) {
        Product product = new Product();
        product.setProductId(productId);
        product.setTitle("legacy-" + productId);
        product.setBlockchainStatus(null);
        product.setBlockchainRetryCount(null);
        return product;
    }
}
