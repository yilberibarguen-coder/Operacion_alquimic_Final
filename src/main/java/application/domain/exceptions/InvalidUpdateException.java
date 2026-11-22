package application.domain.exceptions;

public class InvalidUpdateException extends DomainException {
    public InvalidUpdateException(String message) {
        super(message);
    }
}
