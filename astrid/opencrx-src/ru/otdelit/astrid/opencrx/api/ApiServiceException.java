package ru.otdelit.astrid.opencrx.api;

import java.io.IOException;


/**
 * Exception that wraps an exception encountered during API invocation or
 * processing.
 *
 * @author timsu
 *
 */
public class ApiServiceException extends IOException {

    private static final long serialVersionUID = 8805573304840404684L;

    public ApiServiceException(String detailMessage) {
        super(detailMessage);
    }

    public ApiServiceException(Throwable throwable) {
        super(throwable.getMessage());
        initCause(throwable);
    }

    public ApiServiceException() {
        super();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getMessage(); //$NON-NLS-1$
    }

}
