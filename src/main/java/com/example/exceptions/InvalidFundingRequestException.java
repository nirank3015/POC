package com.example.exceptions;

public class InvalidFundingRequestException extends RuntimeException {
    public InvalidFundingRequestException(String message) {
        super(message);
    }
}