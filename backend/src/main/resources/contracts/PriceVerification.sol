// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract PriceVerification {
    // 상품 ID(ProductID)를 기준으로 해시값을 저장하는 매핑
    // mapping(productId => hashString)
    mapping(uint256 => string) public productHashes;

    // 해시값이 기록될 때 발생하는 이벤트 (블록 익스플로러에서 확인 가능)
    event HashRecorded(uint256 indexed productId, string dataHash, uint256 timestamp);

    /**
     * @dev 특정 상품의 해시값을 블록체인에 기록합니다.
     * @param _productId DB에 저장된 상품 고유 ID
     * @param _dataHash 상품의 데이터 해시 (ProductID + ISBN + Price를 SHA-256으로 해싱한 문자열)
     */
    function recordHash(uint256 _productId, string memory _dataHash) public {
        productHashes[_productId] = _dataHash;
        emit HashRecorded(_productId, _dataHash, block.timestamp);
    }

    /**
     * @dev 블록체인에 기록된 특정 상품의 원본 해시값을 조회합니다.
     * @param _productId 조회할 상품 ID
     * @return 기록된 해시 문자열
     */
    function getHash(uint256 _productId) public view returns (string memory) {
        return productHashes[_productId];
    }
}
