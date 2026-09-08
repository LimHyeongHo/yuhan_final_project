package com.Nbbang.backend.domain.product.service;

import com.Nbbang.backend.domain.product.entity.BlockchainJobStatus;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionDecoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class BlockchainServiceTest {

    private static final String DATA_HASH = "a".repeat(64);
    private static final String CONTRACT = "0x1111111111111111111111111111111111111111";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger pendingNonce = new AtomicInteger(7);
    private final List<Integer> submittedNonces = new CopyOnWriteArrayList<>();
    private final Map<Long, Product> products = new ConcurrentHashMap<>();

    private HttpServer rpcServer;
    private BlockchainService blockchainService;
    private ProductHashService productHashService;

    @BeforeEach
    void setUp() throws IOException {
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        products.put(1L, product(1L));
        products.put(2L, product(2L));
        when(productRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(products.get(invocation.getArgument(0))));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        rpcServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        rpcServer.createContext("/", this::handleRpc);
        rpcServer.setExecutor(Executors.newCachedThreadPool());
        rpcServer.start();

        productHashService = Mockito.mock(ProductHashService.class);
        blockchainService = new BlockchainService(productRepository, productHashService);
        ReflectionTestUtils.setField(blockchainService, "blockchainUrl",
                "http://127.0.0.1:" + rpcServer.getAddress().getPort());
        ReflectionTestUtils.setField(blockchainService, "privateKey", "1".repeat(64));
        ReflectionTestUtils.setField(blockchainService, "contractAddress", CONTRACT);
        ReflectionTestUtils.setField(blockchainService, "connectTimeoutMillis", 500L);
        ReflectionTestUtils.setField(blockchainService, "readTimeoutMillis", 500L);
        ReflectionTestUtils.setField(blockchainService, "callTimeoutMillis", 1000L);
        ReflectionTestUtils.setField(blockchainService, "maxRetryAttempts", 2);
        ReflectionTestUtils.setField(blockchainService, "retryBackoffMillis", 0L);
        ReflectionTestUtils.setField(blockchainService, "receiptPollIntervalMillis", 100L);
        ReflectionTestUtils.setField(blockchainService, "receiptPollAttempts", 2);
    }

    @AfterEach
    void tearDown() {
        if (rpcServer != null) {
            rpcServer.stop(0);
        }
    }

    @Test
    void concurrentProductsReceiveConsecutiveNoncesAndTheirOwnTransactionHashes() throws Exception {
        CompletableFuture<BlockchainService.BlockchainWriteResult> first =
                CompletableFuture.supplyAsync(() -> blockchainService.recordHashAndConfirm(1L, DATA_HASH));
        CompletableFuture<BlockchainService.BlockchainWriteResult> second =
                CompletableFuture.supplyAsync(() -> blockchainService.recordHashAndConfirm(2L, DATA_HASH));

        List<BlockchainService.BlockchainWriteResult> results = List.of(
                first.get(5, TimeUnit.SECONDS),
                second.get(5, TimeUnit.SECONDS));

        assertThat(results).allMatch(BlockchainService.BlockchainWriteResult::success);
        assertThat(new ArrayList<>(submittedNonces)).containsExactly(7, 8);
        assertThat(products.get(1L).getTxHash()).isNotBlank();
        assertThat(products.get(2L).getTxHash()).isNotBlank();
        assertThat(products.get(1L).getTxHash()).isNotEqualTo(products.get(2L).getTxHash());
        assertThat(products.get(1L).getBlockchainStatus()).isEqualTo(BlockchainJobStatus.CONFIRMED);
        assertThat(products.get(2L).getBlockchainStatus()).isEqualTo(BlockchainJobStatus.CONFIRMED);
    }

    @Test
    void requiredRpcErrorsAreClassifiedForLimitedRetry() {
        assertThat(BlockchainService.classifyRpcError("nonce too low").retryable()).isTrue();
        assertThat(BlockchainService.classifyRpcError(
                "replacement transaction underpriced").retryable()).isTrue();
        assertThat(BlockchainService.classifyRpcError("network timeout").retryable()).isTrue();
        assertThat(BlockchainService.classifyRpcError("execution reverted").retryable()).isFalse();
    }

    @Test
    void resumingRetryableJobPreservesRetryCount() {
        Product retryableProduct = product(3L);
        retryableProduct.setBlockchainStatus(BlockchainJobStatus.FAILED_RETRYABLE);
        retryableProduct.setBlockchainRetryCount(1);
        products.put(3L, retryableProduct);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        when(productRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(products.get(invocation.getArgument(0))));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findByBlockchainStatusIn(any()))
                .thenReturn(List.of(retryableProduct));
        when(productHashService.calculateHash(retryableProduct)).thenReturn(DATA_HASH);

        blockchainService = new BlockchainService(productRepository, productHashService);
        ReflectionTestUtils.setField(blockchainService, "blockchainUrl",
                "http://127.0.0.1:" + rpcServer.getAddress().getPort());
        ReflectionTestUtils.setField(blockchainService, "privateKey", "1".repeat(64));
        ReflectionTestUtils.setField(blockchainService, "contractAddress", CONTRACT);
        ReflectionTestUtils.setField(blockchainService, "connectTimeoutMillis", 500L);
        ReflectionTestUtils.setField(blockchainService, "readTimeoutMillis", 500L);
        ReflectionTestUtils.setField(blockchainService, "callTimeoutMillis", 1000L);
        ReflectionTestUtils.setField(blockchainService, "maxRetryAttempts", 2);
        ReflectionTestUtils.setField(blockchainService, "retryBackoffMillis", 0L);
        ReflectionTestUtils.setField(blockchainService, "receiptPollIntervalMillis", 100L);
        ReflectionTestUtils.setField(blockchainService, "receiptPollAttempts", 2);

        blockchainService.resumeIncompleteJobs();

        assertThat(retryableProduct.getBlockchainStatus())
                .isEqualTo(BlockchainJobStatus.CONFIRMED);
        assertThat(retryableProduct.getBlockchainRetryCount()).isEqualTo(1);
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setProductId(id);
        product.setTitle("product-" + id);
        product.setBlockchainStatus(BlockchainJobStatus.QUEUED);
        product.setBlockchainRetryCount(0);
        return product;
    }

    private void handleRpc(HttpExchange exchange) throws IOException {
        JsonNode request = objectMapper.readTree(exchange.getRequestBody());
        String method = request.path("method").asText();
        JsonNode result;

        switch (method) {
            case "eth_getTransactionCount" -> result = objectMapper.getNodeFactory()
                    .textNode("0x" + Integer.toHexString(pendingNonce.get()));
            case "eth_gasPrice" -> result = objectMapper.getNodeFactory().textNode("0x3b9aca00");
            case "eth_sendRawTransaction" -> {
                String encodedTransaction = request.path("params").get(0).asText();
                RawTransaction transaction = TransactionDecoder.decode(encodedTransaction);
                int nonce = transaction.getNonce().intValueExact();
                submittedNonces.add(nonce);
                pendingNonce.accumulateAndGet(nonce + 1, Math::max);
                result = objectMapper.getNodeFactory().textNode(Hash.sha3(encodedTransaction));
            }
            case "eth_getTransactionReceipt" -> result = receipt(request.path("params").get(0).asText());
            case "eth_call" -> result = objectMapper.getNodeFactory().textNode(
                    abiEncodedString(DATA_HASH));
            default -> throw new IllegalArgumentException("Unexpected JSON-RPC method: " + method);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", request.get("id"));
        response.set("result", result);
        byte[] body = objectMapper.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private ObjectNode receipt(String txHash) {
        ObjectNode receipt = objectMapper.createObjectNode();
        receipt.put("transactionHash", txHash);
        receipt.put("transactionIndex", "0x0");
        receipt.put("blockHash", "0x" + "0".repeat(64));
        receipt.put("blockNumber", "0x1");
        receipt.put("cumulativeGasUsed", "0x5208");
        receipt.put("gasUsed", "0x5208");
        receipt.putNull("contractAddress");
        receipt.put("status", "0x1");
        receipt.put("from", "0x" + "2".repeat(40));
        receipt.put("to", CONTRACT);
        receipt.putArray("logs");
        receipt.put("logsBloom", "0x" + "0".repeat(512));
        return receipt;
    }

    private String abiEncodedString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int paddedLength = ((bytes.length + 31) / 32) * 32;
        String offset = String.format("%064x", BigInteger.valueOf(32));
        String length = String.format("%064x", BigInteger.valueOf(bytes.length));
        String data = java.util.HexFormat.of().formatHex(bytes);
        return "0x" + offset + length + data + "0".repeat((paddedLength - bytes.length) * 2);
    }
}
