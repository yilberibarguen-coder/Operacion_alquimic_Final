package application.domain.exceptions;

/** Base of every business rule violation. It knows nothing about HTTP. */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
