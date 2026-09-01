package com.clas.catalog.service;

public class InternalAuthenticationException extends RuntimeException {
    public InternalAuthenticationException() {
        super("internal service authentication failed");
    }
}
