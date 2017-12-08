package com.chattree.chattree.websocket;

public interface WebSocketCaller {

    WebSocketService getWebSocketService();

    void attemptToSendMessage(String messageContent);

    void attemptToJoinThreadRoom(int threadId);
}
