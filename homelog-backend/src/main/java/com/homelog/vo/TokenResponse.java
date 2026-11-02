package com.homelog.vo;

import lombok.Data;

/**
 * Token 响应
 */
@Data
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn; // access token 过期时间(秒)

    public TokenResponse(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn / 1000; // 转换为秒
    }
}