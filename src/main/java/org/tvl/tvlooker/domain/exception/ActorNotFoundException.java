package org.tvl.tvlooker.domain.exception;

import lombok.experimental.StandardException;

@StandardException
public class ActorNotFoundException extends RuntimeException {
    public ActorNotFoundException(String message) {
        super(message);
    }
}
