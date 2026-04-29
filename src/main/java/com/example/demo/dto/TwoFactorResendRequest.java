package com.example.demo.dto;

import lombok.Data;

@Data
public class TwoFactorResendRequest {
    private String temporaryToken;
}
