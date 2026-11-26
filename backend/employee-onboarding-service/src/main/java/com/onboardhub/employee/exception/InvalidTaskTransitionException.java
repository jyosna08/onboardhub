package com.onboardhub.employee.exception;

public class InvalidTaskTransitionException extends RuntimeException {
    public InvalidTaskTransitionException(String message) { super(message); }
}
