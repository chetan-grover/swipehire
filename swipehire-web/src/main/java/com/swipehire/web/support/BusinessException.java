package com.swipehire.web.support;

/**
 * A rule was broken by the user (quota exceeded, illegal status change...). The message is
 * shown to the user as-is. Being a RuntimeException, it also rolls back the transaction.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
