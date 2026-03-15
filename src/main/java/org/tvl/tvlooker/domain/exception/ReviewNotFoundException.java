package org.tvl.tvlooker.domain.exception;

import lombok.experimental.StandardException;

@StandardException
public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(String message) {
        super(message);
    }
}
