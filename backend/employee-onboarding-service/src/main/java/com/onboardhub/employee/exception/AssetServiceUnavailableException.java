package com.onboardhub.employee.exception;

public class AssetServiceUnavailableException extends RuntimeException {
    public AssetServiceUnavailableException() {
        super("Asset service is unavailable, please try again");
    }
}
