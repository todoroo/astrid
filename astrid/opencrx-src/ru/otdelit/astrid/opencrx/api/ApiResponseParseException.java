package ru.otdelit.astrid.opencrx.api;


/**
 * Exception that wraps an exception encountered during API invocation or
 * processing.
 *
 * @author timsu
 *
 */
public class ApiResponseParseException extends ApiServiceException {

    private static final long serialVersionUID = 5421855785088364483L;

    public ApiResponseParseException(Throwable throwable) {
        super("Exception reading API response: " + throwable.getMessage()); //$NON-NLS-1$
        initCause(throwable);
    }
}
