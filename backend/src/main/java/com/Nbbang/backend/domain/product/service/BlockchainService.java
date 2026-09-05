package com.Nbbang.backend.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;
import org.web3j.protocol.core.DefaultBlockParameterName;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.product.entity.Product;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockchainService {

    private static final long RECEIPT_POLL_INTERVAL_MILLIS = 3_000L;
    private static final int RECEIPT_POLL_ATTEMPTS = 40;

    private final ProductRepository productRepository;

    @Value("${blockchain.url}")
    private String blockchainUrl;

    @Value("${blockchain.private-key}")
    private String privateKey;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    /**
     * 비동기로 블록체인에 상품 해시를 기록합니다.
     * 트랜잭션 전송 후 영수증 및 온체인 저장값을 확인합니다.
     * 
     * @param productId 상품 ID
     * @param dataHash (ProductID + ISBN + Price)의 SHA-256 해시값
    */
    @Async
    public void recordHashAsync(Long productId, String dataHash) {
        BlockchainWriteResult result = recordHashAndConfirm(productId, dataHash);
        if (!result.success()) {
            System.err.println("상품 " + productId + " 블록체인 기록 실패: " + result.message());
        }
    }

    /**
     * 트랜잭션을 전송하고 영수증 성공 및 온체인 해시 일치까지 확인합니다.
     * 수동 Nonce를 사용하므로 동일 지갑의 동시 전송을 직렬화합니다.
     */
    public synchronized BlockchainWriteResult recordHashAndConfirm(Long productId, String dataHash) {
        if (privateKey == null || privateKey.contains("0000000000000")) {
            return BlockchainWriteResult.failure(null, "블록체인 Private Key가 설정되지 않았습니다.");
        }
        if (contractAddress == null || contractAddress.contains("0x000000")) {
            return BlockchainWriteResult.failure(null, "블록체인 컨트랙트 주소가 설정되지 않았습니다.");
        }

        Web3j web3j = null;
        String submittedTxHash = null;
        try {
            web3j = Web3j.build(new HttpService(blockchainUrl));
            Credentials credentials = Credentials.create(privateKey);
            
            // 스마트 컨트랙트 함수 정의: recordHash(uint256, string)
            Function function = new Function(
                    "recordHash",
                    Arrays.asList(new Uint256(productId), new Utf8String(dataHash)),
                    Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);
            
            // 트랜잭션 파라미터 (단순화: 가스 제한, 가스 가격 등은 최신 Web3j TransactionManager를 쓰거나 하드코딩)
            BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING).send().getTransactionCount();
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger gasLimit = BigInteger.valueOf(3000000L); // 넉넉하게 잡음

            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce, gasPrice, gasLimit, contractAddress, encodedFunction);

            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            
            if (ethSendTransaction.hasError()) {
                String errorMessage = ethSendTransaction.getError().getMessage();
                if (errorMessage != null && errorMessage.toLowerCase().contains("txpool is full")) {
                    return BlockchainWriteResult.txPoolFull(errorMessage);
                }
                return BlockchainWriteResult.failure(null, "트랜잭션 전송 오류: " + errorMessage);
            }
            
            submittedTxHash = ethSendTransaction.getTransactionHash();

            // 전송 추적을 위해 txHash를 먼저 저장하되, 완료 판정은 영수증과 온체인 값을 기준으로 한다.
            saveTransactionHash(productId, submittedTxHash);
            return waitForConfirmation(web3j, productId, dataHash, submittedTxHash);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("was not generated after")) {
                return BlockchainWriteResult.timeout(submittedTxHash, e.getMessage());
            }
            return BlockchainWriteResult.failure(submittedTxHash,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            if (web3j != null) {
                web3j.shutdown();
            }
        }
    }

    /**
     * 과거 실행에서 이미 전송된 트랜잭션의 현재 상태를 한 번만 조회합니다.
     * 아직 채굴되지 않은 거래는 기다리지 않고 PENDING으로 반환합니다.
     */
    public synchronized BlockchainWriteResult confirmExistingTransaction(
            Long productId, String dataHash, String txHash) {
        if (txHash == null || txHash.isBlank()) {
            return BlockchainWriteResult.failure(null, "확인할 트랜잭션 해시가 없습니다.");
        }

        Web3j web3j = null;
        try {
            web3j = Web3j.build(new HttpService(blockchainUrl));
            EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(txHash).send();
            if (response.hasError()) {
                return BlockchainWriteResult.failure(txHash,
                        "트랜잭션 영수증 조회 오류: " + response.getError().getMessage());
            }
            if (response.getTransactionReceipt().isEmpty()) {
                return BlockchainWriteResult.pending(txHash);
            }
            return validateReceipt(web3j, productId, dataHash,
                    txHash, response.getTransactionReceipt().get());
        } catch (Exception e) {
            return BlockchainWriteResult.failure(txHash,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            if (web3j != null) {
                web3j.shutdown();
            }
        }
    }

    private BlockchainWriteResult waitForConfirmation(
            Web3j web3j, Long productId, String dataHash, String txHash) throws Exception {
        PollingTransactionReceiptProcessor receiptProcessor =
                new PollingTransactionReceiptProcessor(
                        web3j, RECEIPT_POLL_INTERVAL_MILLIS, RECEIPT_POLL_ATTEMPTS);
        TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);

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
            return BlockchainWriteResult.failure(txHash,
                    "트랜잭션은 확정되었지만 온체인 검증에 실패했습니다: " + readResult.message());
        }

        String hashOnChain = readResult.hash();
        if (hashOnChain == null || hashOnChain.isEmpty()) {
            return BlockchainWriteResult.failure(txHash,
                    "트랜잭션은 확정되었지만 컨트랙트에 저장된 해시가 비어 있습니다.");
        }

        if (!normalizeHash(dataHash).equals(normalizeHash(hashOnChain))) {
            return BlockchainWriteResult.failure(txHash,
                    "트랜잭션은 확정되었지만 저장된 온체인 해시가 요청한 해시와 다릅니다.");
        }

        return BlockchainWriteResult.success(txHash);
    }

    private void saveTransactionHash(Long productId, String txHash) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            product.setTxHash(txHash);
            productRepository.save(product);
        }
    }

    /**
     * 블록체인에 기록된 상품 해시를 조회합니다. (Read-only, 수수료 없음)
     * @param productId 상품 ID
     * @return 기록된 해시 문자열
     */
    public String getHash(Long productId) {
        BlockchainReadResult result = readHash(productId);
        return result.success() ? result.hash() : null;
    }

    public BlockchainReadResult readHash(Long productId) {
        if (contractAddress == null || contractAddress.contains("0x000000")) {
            return BlockchainReadResult.failure("블록체인 컨트랙트 주소가 설정되지 않았습니다.");
        }

        Web3j web3j = null;
        try {
            web3j = Web3j.build(new HttpService(blockchainUrl));
            return readHash(web3j, productId);
        } catch (Exception e) {
            return BlockchainReadResult.failure(
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            if (web3j != null) {
                web3j.shutdown();
            }
        }
    }

    private BlockchainReadResult readHash(Web3j web3j, Long productId) throws Exception {
        Function function = new Function(
                "getHash",
                Arrays.asList(new Uint256(productId)),
                Arrays.asList(new TypeReference<Utf8String>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .send();

        if (response.hasError()) {
            return BlockchainReadResult.failure(response.getError().getMessage());
        }
        if (response.getValue() == null || response.getValue().equals("0x")) {
            return BlockchainReadResult.failure("컨트랙트 호출 결과가 비어 있습니다.");
        }

        List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        if (results.isEmpty()) {
            return BlockchainReadResult.failure("컨트랙트 응답을 해석할 수 없습니다.");
        }

        return BlockchainReadResult.success((String) results.get(0).getValue());
    }

    private String normalizeHash(String hash) {
        return hash.replace("0x", "")
                .trim()
                .toLowerCase()
                .replaceAll("[^a-f0-9]", "");
    }

    public record BlockchainWriteResult(
            boolean success, String txHash, String message, String code) {

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

        public static BlockchainWriteResult txPoolFull(String message) {
            return new BlockchainWriteResult(false, null,
                    "트랜잭션 전송 오류: " + message, "TXPOOL_FULL");
        }
    }

    public record BlockchainReadResult(boolean success, String hash, String message) {

        public static BlockchainReadResult success(String hash) {
            return new BlockchainReadResult(true, hash, null);
        }

        public static BlockchainReadResult failure(String message) {
            return new BlockchainReadResult(false, null, message);
        }
    }
}
