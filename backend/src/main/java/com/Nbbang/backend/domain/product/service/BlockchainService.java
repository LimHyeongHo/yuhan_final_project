package com.Nbbang.backend.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
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
import org.web3j.utils.Numeric;
import org.web3j.protocol.core.DefaultBlockParameterName;
import com.Nbbang.backend.domain.product.repository.ProductRepository;
import com.Nbbang.backend.domain.product.entity.Product;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockchainService {

    private final ProductRepository productRepository;

    @Value("${blockchain.url}")
    private String blockchainUrl;

    @Value("${blockchain.private-key}")
    private String privateKey;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    /**
     * 비동기로 블록체인에 상품 해시를 기록합니다.
     * 트랜잭션 전송 후 txHash를 반환하거나 DB에 업데이트하는 콜백 처리를 할 수 있습니다.
     * 여기서는 단순화를 위해 트랜잭션 전송 로직만 구현합니다.
     * 
     * @param productId 상품 ID
     * @param dataHash (ProductID + ISBN + Price)의 SHA-256 해시값
     * @return 트랜잭션 해시(TxHash)
     */
    @Async
    @Transactional
    public void recordHashAsync(Long productId, String dataHash) {
        if (privateKey == null || privateKey.contains("0000000000000")) {
            System.err.println("블록체인 Private Key가 설정되지 않았습니다. 트랜잭션 전송을 건너뜁니다.");
            return;
        }
        
        try {
            Web3j web3j = Web3j.build(new HttpService(blockchainUrl));
            Credentials credentials = Credentials.create(privateKey);
            
            // 스마트 컨트랙트 함수 정의: recordHash(uint256, string)
            Function function = new Function(
                    "recordHash",
                    Arrays.asList(new Uint256(productId), new Utf8String(dataHash)),
                    Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);
            
            // 트랜잭션 파라미터 (단순화: 가스 제한, 가스 가격 등은 최신 Web3j TransactionManager를 쓰거나 하드코딩)
            BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).send().getTransactionCount();
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger gasLimit = BigInteger.valueOf(3000000L); // 넉넉하게 잡음

            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce, gasPrice, gasLimit, contractAddress, encodedFunction);

            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            
            if (ethSendTransaction.hasError()) {
                System.err.println("트랜잭션 에러: " + ethSendTransaction.getError().getMessage());
                return;
            }
            
            String txHash = ethSendTransaction.getTransactionHash();
            
            // DB에 트랜잭션 해시 업데이트
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                product.setTxHash(txHash);
                productRepository.save(product);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 블록체인에 기록된 상품 해시를 조회합니다. (Read-only, 수수료 없음)
     * @param productId 상품 ID
     * @return 기록된 해시 문자열
     */
    public String getHash(Long productId) {
        if (contractAddress == null || contractAddress.contains("0x000000")) {
            return null;
        }

        try {
            Web3j web3j = Web3j.build(new HttpService(blockchainUrl));
            
            // 스마트 컨트랙트 함수 정의: getHash(uint256) returns (string)
            Function function = new Function(
                    "getHash",
                    Arrays.asList(new Uint256(productId)),
                    Arrays.asList(new TypeReference<Utf8String>() {})
            );

            String encodedFunction = FunctionEncoder.encode(function);
            
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
            
            if (response.hasError() || response.getValue() == null || response.getValue().equals("0x")) {
                return null;
            }
            
            List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
            if (results.isEmpty()) return null;
            
            return (String) results.get(0).getValue();
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
