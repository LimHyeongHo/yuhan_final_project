package com.Nbbang.backend.domain.product.service;

import com.Nbbang.backend.domain.product.entity.BlockchainJobStatus;
import com.Nbbang.backend.domain.product.entity.Product;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(3_000_000L);

    private final ProductRepository productRepository;
    private final ProductHashService productHashService;
    private final ReentrantLock submissionLock = new ReentrantLock(true);

    @Value("${blockchain.url}")
    private String blockchainUrl;

    @Value("${blockchain.private-key}")
    private String privateKey;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    @Value("${blockchain.rpc.connect-timeout-ms:3000}")
    private long connectTimeoutMillis;

    @Value("${blockchain.rpc.read-timeout-ms:5000}")
    private long readTimeoutMillis;

    @Value("${blockchain.rpc.call-timeout-ms:8000}")
    private long callTimeoutMillis;

    @Value("${blockchain.retry.max-attempts:2}")
    private int maxRetryAttempts;

    @Value("${blockchain.retry.backoff-ms:500}")
    private long retryBackoffMillis;

    @Value("${blockchain.receipt.poll-interval-ms:3000}")
    private long receiptPollIntervalMillis;

    @Value("${blockchain.receipt.poll-attempts:40}")
    private int receiptPollAttempts;

    public BlockchainService(
            ProductRepository productRepository,
            ProductHashService productHashService) {
        this.productRepository = productRepository;
        this.productHashService = productHashService;
    }

    /** 재시작 전 전송·처리가 끝나지 않은 작업을 DB 상태를 기준으로 다시 확인한다. */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void resumeIncompleteJobs() {
        List<Product> incompleteProducts = productRepository.findByBlockchainStatusIn(List.of(
                BlockchainJobStatus.QUEUED,
                BlockchainJobStatus.SUBMITTED,
                BlockchainJobStatus.FAILED_RETRYABLE));

        for (Product product : incompleteProducts) {
            try {
                String dataHash = productHashService.calculateHash(product);
                if (product.getTxHash() != null && !product.getTxHash().isBlank()) {
                    confirmExistingTransaction(product.getProductId(), dataHash, product.getTxHash());
                } else {
                    recordHashAndConfirm(product.getProductId(), dataHash, false);
                }
            } catch (RuntimeException e) {
                updateJobState(product.getProductId(), BlockchainJobStatus.FAILED_FINAL,
                        null, product.getBlockchainRetryCount(), errorMessage(e));
                log.error("Failed to resume blockchain job. productId={}",
                        product.getProductId(), e);
            }
        }
    }

    /** 상품 등록 요청과 분리된 비동기 작업. 실패 원인은 반드시 DB에 남긴다. */
    @Async
    public void recordHashAsync(Long productId, String dataHash) {
        try {
            BlockchainWriteResult result = recordHashAndConfirm(productId, dataHash);
            if (!result.success()) {
                log.warn("Blockchain anchoring failed. productId={}, code={}, message={}",
                        productId, result.code(), result.message());
            }
        } catch (RuntimeException e) {
            updateJobState(productId, BlockchainJobStatus.FAILED_FINAL, null, 0, errorMessage(e));
            log.error("Unexpected blockchain anchoring failure. productId={}", productId, e);
        }
    }

    /**
     * 송신 구간만 공정 잠금으로 직렬화한다. 영수증 대기는 잠금 밖에서 수행하므로
     * 앞선 Sepolia 트랜잭션의 채굴이 다음 상품의 nonce 배정을 막지 않는다.
     */
    public BlockchainWriteResult recordHashAndConfirm(Long productId, String dataHash) {
        return recordHashAndConfirm(productId, dataHash, true);
    }

    private BlockchainWriteResult recordHashAndConfirm(
            Long productId, String dataHash, boolean resetRetryCount) {
        int initialRetryCount = resetRetryCount ? 0 : currentRetryCount(productId);
        updateJobState(productId, BlockchainJobStatus.QUEUED, null, initialRetryCount, null);

        if (!hasBlockchainConfiguration()) {
            BlockchainWriteResult result = BlockchainWriteResult.failure(
                    null, "블록체인 연결 설정이 올바르지 않습니다.");
            applyWriteResult(productId, result, initialRetryCount);
            return result;
        }

        SubmissionResult submission = submitSerialized(productId, dataHash, initialRetryCount);
        if (!submission.accepted()) {
            return submission.result();
        }

        Web3j web3j = null;
        BlockchainWriteResult result;
        try {
            web3j = createWeb3j();
            result = waitForConfirmation(
                    web3j, productId, dataHash, submission.result().txHash());
        } catch (Exception e) {
            String message = errorMessage(e);
            result = isReceiptTimeout(message) || classifyRpcError(message) == RpcErrorType.NETWORK
                    ? BlockchainWriteResult.timeout(submission.result().txHash(), message)
                    : BlockchainWriteResult.failure(submission.result().txHash(), message);
        } finally {
            shutdown(web3j);
        }

        applyWriteResult(productId, result, submission.retryCount());
        return result;
    }

    private SubmissionResult submitSerialized(
            Long productId, String dataHash, int initialRetryCount) {
        submissionLock.lock();
        Web3j web3j = null;
        try {
            web3j = createWeb3j();
            Credentials credentials = Credentials.create(privateKey);
            Function function = new Function(
                    "recordHash",
                    Arrays.asList(new Uint256(productId), new Utf8String(dataHash)),
                    Collections.emptyList());
            String encodedFunction = FunctionEncoder.encode(function);

            BigInteger nonce = null;
            BigInteger gasPrice = null;
            int setupRetryCount = 0;
            for (int setupAttempt = 0;
                 setupAttempt <= Math.max(0, maxRetryAttempts);
                 setupAttempt++) {
                try {
                    nonce = pendingNonce(web3j, credentials);
                    gasPrice = gasPrice(web3j);
                    break;
                } catch (Exception e) {
                    String message = errorMessage(e);
                    if (classifyRpcError(message) != RpcErrorType.NETWORK
                            || setupAttempt >= maxRetryAttempts) {
                        BlockchainWriteResult failure = BlockchainWriteResult.failure(null, message);
                        int totalRetryCount = initialRetryCount + setupAttempt;
                        applyWriteResult(productId, failure, totalRetryCount);
                        return SubmissionResult.rejected(failure, totalRetryCount);
                    }
                    setupRetryCount = setupAttempt + 1;
                    updateJobState(productId, BlockchainJobStatus.FAILED_RETRYABLE,
                            null, initialRetryCount + setupRetryCount, message);
                    pauseBeforeRetry();
                }
            }

            if (nonce == null || gasPrice == null) {
                BlockchainWriteResult failure = BlockchainWriteResult.failure(
                        null, "트랜잭션 전송에 필요한 RPC 정보를 조회하지 못했습니다.");
                int totalRetryCount = initialRetryCount + setupRetryCount;
                applyWriteResult(productId, failure, totalRetryCount);
                return SubmissionResult.rejected(failure, totalRetryCount);
            }

            boolean ambiguousNetworkFailure = false;

            for (int retryCount = 0; retryCount <= Math.max(0, maxRetryAttempts); retryCount++) {
                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce, gasPrice, GAS_LIMIT, contractAddress, encodedFunction);
                String signedTransaction = Numeric.toHexString(
                        TransactionEncoder.signMessage(rawTransaction, credentials));
                String localTxHash = Hash.sha3(signedTransaction);

                try {
                    EthSendTransaction response = web3j.ethSendRawTransaction(signedTransaction).send();
                    if (!response.hasError()) {
                        String txHash = response.getTransactionHash();
                        int totalRetryCount = initialRetryCount + setupRetryCount + retryCount;
                        updateJobState(productId, BlockchainJobStatus.SUBMITTED,
                                txHash, totalRetryCount, null);
                        return SubmissionResult.accepted(txHash, totalRetryCount);
                    }

                    String message = response.getError() == null
                            ? "트랜잭션 전송 RPC가 오류를 반환했습니다."
                            : response.getError().getMessage();
                    RpcErrorType errorType = classifyRpcError(message);
                    if (errorType == RpcErrorType.ALREADY_KNOWN
                            || (ambiguousNetworkFailure && errorType == RpcErrorType.NONCE_TOO_LOW)) {
                        int totalRetryCount = initialRetryCount + setupRetryCount + retryCount;
                        updateJobState(productId, BlockchainJobStatus.SUBMITTED,
                                localTxHash, totalRetryCount, null);
                        return SubmissionResult.accepted(localTxHash, totalRetryCount);
                    }
                    if (!errorType.retryable() || retryCount >= maxRetryAttempts) {
                        BlockchainWriteResult failure = BlockchainWriteResult.failure(null,
                                "트랜잭션 전송 오류: " + message);
                        int totalRetryCount = initialRetryCount + setupRetryCount + retryCount;
                        applyWriteResult(productId, failure, totalRetryCount);
                        return SubmissionResult.rejected(failure, totalRetryCount);
                    }

                    int nextRetryCount = initialRetryCount + setupRetryCount + retryCount + 1;
                    updateJobState(productId, BlockchainJobStatus.FAILED_RETRYABLE,
                            null, nextRetryCount, message);
                    if (errorType == RpcErrorType.NONCE_TOO_LOW) {
                        nonce = pendingNonce(web3j, credentials);
                    } else if (errorType == RpcErrorType.REPLACEMENT_UNDERPRICED) {
                        gasPrice = gasPrice.multiply(BigInteger.valueOf(112))
                                .divide(BigInteger.valueOf(100));
                    }
                    pauseBeforeRetry();
                } catch (Exception e) {
                    String message = errorMessage(e);
                    if (classifyRpcError(message) != RpcErrorType.NETWORK
                            || retryCount >= maxRetryAttempts) {
                        BlockchainWriteResult failure = BlockchainWriteResult.failure(null, message);
                        int totalRetryCount = initialRetryCount + setupRetryCount + retryCount;
                        applyWriteResult(productId, failure, totalRetryCount);
                        return SubmissionResult.rejected(failure, totalRetryCount);
                    }
                    ambiguousNetworkFailure = true;
                    updateJobState(productId, BlockchainJobStatus.FAILED_RETRYABLE,
                            null, initialRetryCount + setupRetryCount + retryCount + 1, message);
                    pauseBeforeRetry();
                }
            }

            BlockchainWriteResult failure = BlockchainWriteResult.failure(
                    null, "블록체인 트랜잭션 재시도 횟수를 초과했습니다.");
            int totalRetryCount = initialRetryCount + setupRetryCount + Math.max(0, maxRetryAttempts);
            applyWriteResult(productId, failure, totalRetryCount);
            return SubmissionResult.rejected(failure, totalRetryCount);
        } catch (Exception e) {
            BlockchainWriteResult failure = BlockchainWriteResult.failure(null, errorMessage(e));
            applyWriteResult(productId, failure, initialRetryCount);
            return SubmissionResult.rejected(failure, initialRetryCount);
        } finally {
            shutdown(web3j);
            submissionLock.unlock();
        }
    }

    /** 과거 실행에서 전송된 txHash는 재송신하지 않고 영수증만 확인한다. */
    public BlockchainWriteResult confirmExistingTransaction(
            Long productId, String dataHash, String txHash) {
        if (txHash == null || txHash.isBlank()) {
            BlockchainWriteResult result = BlockchainWriteResult.failure(
                    null, "확인할 트랜잭션 해시가 없습니다.");
            applyWriteResult(productId, result, 0);
            return result;
        }

        updateJobState(productId, BlockchainJobStatus.SUBMITTED, txHash, null, null);
        Web3j web3j = null;
        BlockchainWriteResult result;
        try {
            web3j = createWeb3j();
            EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(txHash).send();
            if (response.hasError()) {
                result = BlockchainWriteResult.unavailable(
                        txHash, "트랜잭션 영수증 조회 오류: " + response.getError().getMessage());
            } else if (response.getTransactionReceipt().isEmpty()) {
                result = BlockchainWriteResult.pending(txHash);
            } else {
                result = validateReceipt(
                        web3j, productId, dataHash, txHash,
                        response.getTransactionReceipt().get());
            }
        } catch (Exception e) {
            result = BlockchainWriteResult.unavailable(txHash, errorMessage(e));
        } finally {
            shutdown(web3j);
        }

        applyWriteResult(productId, result, currentRetryCount(productId));
        return result;
    }

    private BlockchainWriteResult waitForConfirmation(
            Web3j web3j, Long productId, String dataHash, String txHash) throws Exception {
        PollingTransactionReceiptProcessor processor = new PollingTransactionReceiptProcessor(
                web3j,
                Math.max(100L, receiptPollIntervalMillis),
                Math.max(1, receiptPollAttempts));
        TransactionReceipt receipt = processor.waitForTransactionReceipt(txHash);
        return validateReceipt(web3j, productId, dataHash, txHash, receipt);
    }

    private BlockchainWriteResult validateReceipt(
            Web3j web3j, Long productId, String dataHash,
            String txHash, TransactionReceipt receipt) throws Exception {
        if (!receipt.isStatusOK()) {
            return BlockchainWriteResult.failure(txHash,
                    "트랜잭션이 실패했습니다. receipt status=" + receipt.getStatus());
        }

        BlockchainReadResult readResult = readHash(web3j, productId);
        if (!readResult.success()) {
            return BlockchainWriteResult.unavailable(txHash,
                    "트랜잭션은 확정되었지만 온체인 검증에 실패했습니다: " + readResult.message());
        }
        if (readResult.hash() == null || readResult.hash().isEmpty()) {
            return BlockchainWriteResult.failure(txHash,
                    "트랜잭션은 확정되었지만 컨트랙트에 저장된 해시가 비어 있습니다.");
        }
        if (!normalizeHash(dataHash).equals(normalizeHash(readResult.hash()))) {
            return BlockchainWriteResult.failure(txHash,
                    "트랜잭션은 확정되었지만 저장된 온체인 해시가 요청한 해시와 다릅니다.");
        }
        return BlockchainWriteResult.success(txHash);
    }

    public String getHash(Long productId) {
        BlockchainReadResult result = readHash(productId);
        return result.success() ? result.hash() : null;
    }

    public BlockchainReadResult readHash(Long productId) {
        if (!hasBlockchainConfiguration()) {
            return BlockchainReadResult.unavailable("블록체인 연결 설정이 올바르지 않습니다.");
        }

        Web3j web3j = null;
        try {
            web3j = createWeb3j();
            return readHash(web3j, productId);
        } catch (Exception e) {
            return BlockchainReadResult.unavailable(errorMessage(e));
        } finally {
            shutdown(web3j);
        }
    }

    private BlockchainReadResult readHash(Web3j web3j, Long productId) throws Exception {
        Function function = new Function(
                "getHash",
                List.of(new Uint256(productId)),
                List.of(new TypeReference<Utf8String>() { }));
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                        Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                        DefaultBlockParameterName.LATEST)
                .send();

        if (response.hasError()) {
            return BlockchainReadResult.unavailable(response.getError().getMessage());
        }
        if (response.getValue() == null || response.getValue().equals("0x")) {
            return BlockchainReadResult.notFound("컨트랙트 호출 결과가 비어 있습니다.");
        }

        List<Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (results.isEmpty()) {
            return BlockchainReadResult.failure("컨트랙트 응답을 해석할 수 없습니다.");
        }
        return BlockchainReadResult.success((String) results.get(0).getValue());
    }

    private Web3j createWeb3j() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Math.max(1L, connectTimeoutMillis), TimeUnit.MILLISECONDS)
                .readTimeout(Math.max(1L, readTimeoutMillis), TimeUnit.MILLISECONDS)
                .writeTimeout(Math.max(1L, readTimeoutMillis), TimeUnit.MILLISECONDS)
                .callTimeout(Math.max(1L, callTimeoutMillis), TimeUnit.MILLISECONDS)
                .build();
        return Web3j.build(new HttpService(blockchainUrl, client, false));
    }

    private BigInteger pendingNonce(Web3j web3j, Credentials credentials) throws IOException {
        EthGetTransactionCount response = web3j.ethGetTransactionCount(
                        credentials.getAddress(), DefaultBlockParameterName.PENDING).send();
        if (response.hasError()) {
            throw new IOException("Pending nonce 조회 실패: " + response.getError().getMessage());
        }
        if (response.getTransactionCount() == null) {
            throw new IOException("Pending nonce 응답이 비어 있습니다.");
        }
        return response.getTransactionCount();
    }

    private BigInteger gasPrice(Web3j web3j) throws IOException {
        EthGasPrice response = web3j.ethGasPrice().send();
        if (response.hasError()) {
            throw new IOException("Gas price 조회 실패: " + response.getError().getMessage());
        }
        if (response.getGasPrice() == null) {
            throw new IOException("Gas price 응답이 비어 있습니다.");
        }
        return response.getGasPrice();
    }

    private void applyWriteResult(Long productId, BlockchainWriteResult result, Integer retryCount) {
        BlockchainJobStatus status;
        if (result.success()) {
            status = BlockchainJobStatus.CONFIRMED;
        } else if ("PENDING".equals(result.code())
                || "TIMEOUT".equals(result.code())
                || "UNAVAILABLE".equals(result.code())) {
            status = BlockchainJobStatus.SUBMITTED;
        } else {
            status = BlockchainJobStatus.FAILED_FINAL;
        }
        updateJobState(productId, status, result.txHash(), retryCount,
                result.success() ? null : result.message());
    }

    private void updateJobState(
            Long productId,
            BlockchainJobStatus status,
            String txHash,
            Integer retryCount,
            String lastError) {
        productRepository.findById(productId).ifPresent(product -> {
            product.setBlockchainStatus(status);
            if (txHash != null && !txHash.isBlank()) {
                product.setTxHash(txHash);
            }
            if (retryCount != null) {
                product.setBlockchainRetryCount(retryCount);
            }
            product.setBlockchainLastError(lastError);
            product.setBlockchainUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
        });
    }

    private int currentRetryCount(Long productId) {
        return productRepository.findById(productId)
                .map(Product::getBlockchainRetryCount)
                .orElse(0);
    }

    private boolean hasBlockchainConfiguration() {
        return privateKey != null
                && !privateKey.isBlank()
                && !privateKey.contains("0000000000000")
                && contractAddress != null
                && !contractAddress.isBlank()
                && !contractAddress.contains("0x000000")
                && blockchainUrl != null
                && !blockchainUrl.isBlank();
    }

    private void pauseBeforeRetry() throws InterruptedException {
        if (retryBackoffMillis > 0) {
            Thread.sleep(retryBackoffMillis);
        }
    }

    private boolean isReceiptTimeout(String message) {
        String normalized = normalizeMessage(message);
        return normalized.contains("was not generated after")
                || normalized.contains("timeout")
                || normalized.contains("timed out");
    }

    static RpcErrorType classifyRpcError(String message) {
        String normalized = normalizeMessage(message);
        if (normalized.contains("already known")) {
            return RpcErrorType.ALREADY_KNOWN;
        }
        if (normalized.contains("nonce too low")) {
            return RpcErrorType.NONCE_TOO_LOW;
        }
        if (normalized.contains("replacement transaction underpriced")) {
            return RpcErrorType.REPLACEMENT_UNDERPRICED;
        }
        if (normalized.contains("txpool is full") || normalized.contains("transaction pool is full")) {
            return RpcErrorType.TXPOOL_FULL;
        }
        if (normalized.contains("timeout")
                || normalized.contains("timed out")
                || normalized.contains("connection")
                || normalized.contains("network")
                || normalized.contains("unexpected end of stream")
                || normalized.contains("http 429")
                || normalized.contains("http 502")
                || normalized.contains("http 503")
                || normalized.contains("http 504")) {
            return RpcErrorType.NETWORK;
        }
        return RpcErrorType.FINAL;
    }

    private static String normalizeMessage(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }

    private String errorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private String normalizeHash(String hash) {
        return hash.replace("0x", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-f0-9]", "");
    }

    private void shutdown(Web3j web3j) {
        if (web3j != null) {
            web3j.shutdown();
        }
    }

    enum RpcErrorType {
        NONCE_TOO_LOW(true),
        REPLACEMENT_UNDERPRICED(true),
        TXPOOL_FULL(true),
        NETWORK(true),
        ALREADY_KNOWN(false),
        FINAL(false);

        private final boolean retryable;

        RpcErrorType(boolean retryable) {
            this.retryable = retryable;
        }

        boolean retryable() {
            return retryable;
        }
    }

    private record SubmissionResult(
            boolean accepted, BlockchainWriteResult result, int retryCount) {

        static SubmissionResult accepted(String txHash, int retryCount) {
            return new SubmissionResult(
                    true, BlockchainWriteResult.submitted(txHash), retryCount);
        }

        static SubmissionResult rejected(BlockchainWriteResult result, int retryCount) {
            return new SubmissionResult(false, result, retryCount);
        }
    }

    public record BlockchainWriteResult(
            boolean success, String txHash, String message, String code) {

        public static BlockchainWriteResult submitted(String txHash) {
            return new BlockchainWriteResult(
                    false, txHash, "트랜잭션이 전송되었습니다.", "SUBMITTED");
        }

        public static BlockchainWriteResult success(String txHash) {
            return new BlockchainWriteResult(
                    true, txHash, "블록체인 기록이 확정되었습니다.", "SUCCESS");
        }

        public static BlockchainWriteResult failure(String txHash, String message) {
            return new BlockchainWriteResult(false, txHash, message, "FAILED");
        }

        public static BlockchainWriteResult pending(String txHash) {
            return new BlockchainWriteResult(false, txHash,
                    "아직 채굴되지 않은 트랜잭션입니다. 잠시 후 다시 확인해주세요.", "PENDING");
        }

        public static BlockchainWriteResult timeout(String txHash, String message) {
            return new BlockchainWriteResult(false, txHash, message, "TIMEOUT");
        }

        public static BlockchainWriteResult unavailable(String txHash, String message) {
            return new BlockchainWriteResult(false, txHash, message, "UNAVAILABLE");
        }

        public static BlockchainWriteResult txPoolFull(String message) {
            return new BlockchainWriteResult(false, null,
                    "트랜잭션 전송 오류: " + message, "TXPOOL_FULL");
        }
    }

    public record BlockchainReadResult(
            boolean success, String hash, String message, String code) {

        public static BlockchainReadResult success(String hash) {
            return new BlockchainReadResult(true, hash, null, "SUCCESS");
        }

        public static BlockchainReadResult notFound(String message) {
            return new BlockchainReadResult(false, null, message, "NOT_FOUND");
        }

        public static BlockchainReadResult unavailable(String message) {
            return new BlockchainReadResult(false, null, message, "UNAVAILABLE");
        }

        public static BlockchainReadResult failure(String message) {
            return new BlockchainReadResult(false, null, message, "FAILED");
        }
    }
}
