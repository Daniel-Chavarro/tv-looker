package org.tvl.tvlooker.domain.exception;

import lombok.experimental.StandardException;

@StandardException
public class GenreNotFoundException extends RuntimeException {
    public GenreNotFoundException(String message) {
        super(message);
    }
}
