package org.tvl.tvlooker.domain.exception;

import lombok.experimental.StandardException;

@StandardException
public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String message) {
        super(message);
    }
}
