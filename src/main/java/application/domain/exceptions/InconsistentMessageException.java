package application.domain.exceptions;

public class InconsistentMessageException extends DomainException {
    public InconsistentMessageException(String message) {
        super(message);
    }
}
