package org.tvl.tvlooker.domain.exception;

import lombok.experimental.StandardException;

@StandardException
public class NoDataProviderException extends RuntimeException {
    public NoDataProviderException(String message) {
        super(message);
    }
}
