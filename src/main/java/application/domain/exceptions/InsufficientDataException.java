package application.domain.exceptions;

public class InsufficientDataException extends DomainException {
    public InsufficientDataException(String message) {
        super(message);
    }
}
