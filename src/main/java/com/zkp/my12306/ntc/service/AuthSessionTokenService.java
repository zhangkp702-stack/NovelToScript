package com.zkp.my12306.ntc.service;

public interface AuthSessionTokenService {

    String createSessionToken(String username);

    boolean isValid(String sessionId);

    String getUsername(String sessionId);

    void refreshTtl(String sessionId);

    void revoke(String sessionId);
}
