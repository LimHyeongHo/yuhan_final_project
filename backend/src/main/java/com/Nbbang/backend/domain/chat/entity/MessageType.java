package com.Nbbang.backend.domain.chat.entity;

public enum MessageType {
    CHAT,   /// 일반 메시지
    IMAGE,  /// 사진
    JOIN,   /// 입장 안내
    LEAVE,  /// 퇴장 안내
    READ,   /// 읽음 이벤트
    DELETE  /// 삭제 이벤트
}
