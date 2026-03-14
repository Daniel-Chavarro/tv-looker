package org.tvl.tvlooker.domain.exception;

import lombok.experimental.StandardException;

@StandardException
public class ListFavoriteNotFoundException extends RuntimeException {
    public ListFavoriteNotFoundException(String message) {
        super(message);
    }
}
