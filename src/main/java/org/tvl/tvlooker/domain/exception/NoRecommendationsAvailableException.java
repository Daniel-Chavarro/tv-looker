package org.tvl.tvlooker.domain.exception;

import lombok.experimental.StandardException;

@StandardException
public class NoRecommendationsAvailableException extends RuntimeException {
    public NoRecommendationsAvailableException(String message) {
        super(message);
    }
}
