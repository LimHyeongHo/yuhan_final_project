package com.springboot.chatting.chat;

public enum MessageType {

    /// 사용자가 메시지 보냄
    CHAT,

    /// 새 사용자가 채팅 참여
    JOIN,

    /// 사용자가 채팅에서 나가기
    LEAVE
}
