package application.domain.exceptions;

public class InvalidMessageException extends DomainException {
    public InvalidMessageException(String message) {
        super(message);
    }
}
