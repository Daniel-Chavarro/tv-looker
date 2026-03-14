package org.tvl.tvlooker.domain.exception;

import lombok.experimental.StandardException;

@StandardException
public class InteractionNotFoundException extends RuntimeException {
    public InteractionNotFoundException(String message) {
        super(message);
    }
}
