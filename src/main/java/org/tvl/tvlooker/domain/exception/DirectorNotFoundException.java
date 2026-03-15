package org.tvl.tvlooker.domain.exception;

import lombok.experimental.StandardException;

@StandardException
public class DirectorNotFoundException extends RuntimeException {
    public DirectorNotFoundException(String message) {
        super(message);
    }
}
