package com.Nbbang.backend.domain.product.service;

import com.Nbbang.backend.domain.log.repository.SystemLogRepository;
import com.Nbbang.backend.domain.product.entity.BlockchainJobStatus;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private BlockchainService blockchainService;
    @Mock private ProductHashService productHashService;
    @Mock private AladdinApiService aladdinApiService;
    @Mock private SystemLogRepository systemLogRepository;

    private VerificationService verificationService;
    private Product product;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(
                productRepository,
                blockchainService,
                productHashService,
                aladdinApiService,
                systemLogRepository);
        product = new Product();
        product.setProductId(1L);
        product.setBlockchainStatus(BlockchainJobStatus.SUBMITTED);
        product.setBlockchainRetryCount(1);
        product.setTxHash("0x1234");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    }

    @Test
    void rpcFailureIsReturnedAsUnavailableWithPersistedJobState() {
        when(blockchainService.readHash(1L))
                .thenReturn(BlockchainService.BlockchainReadResult.unavailable("timeout"));

        Map<String, Object> result = verificationService.verifyProduct(1L);

        assertThat(result)
                .containsEntry("status", "UNAVAILABLE")
                .containsEntry("blockchainStatus", BlockchainJobStatus.SUBMITTED)
                .containsEntry("retryCount", 1)
                .containsEntry("txHash", "0x1234")
                .containsEntry("retryable", true);
    }

    @Test
    void finalFailureIsDistinguishedFromPending() {
        product.setBlockchainStatus(BlockchainJobStatus.FAILED_FINAL);
        product.setBlockchainLastError("invalid contract");
        when(blockchainService.readHash(1L))
                .thenReturn(BlockchainService.BlockchainReadResult.notFound("empty"));

        Map<String, Object> result = verificationService.verifyProduct(1L);

        assertThat(result)
                .containsEntry("status", "FAILED")
                .containsEntry("lastError", "invalid contract")
                .containsEntry("retryable", false);
    }

    @Test
    void emptyOnChainHashIsReturnedAsPending() {
        when(blockchainService.readHash(1L))
                .thenReturn(BlockchainService.BlockchainReadResult.success(""));

        Map<String, Object> result = verificationService.verifyProduct(1L);

        assertThat(result)
                .containsEntry("status", "PENDING")
                .containsEntry("retryable", true);
    }
}
