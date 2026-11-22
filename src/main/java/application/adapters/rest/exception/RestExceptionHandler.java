package application.adapters.rest.exception;

import application.adapters.rest.dtos.responses.ErrorResponse;
import application.domain.exceptions.DomainException;
import application.domain.exceptions.InconsistentDistancesException;
import application.domain.exceptions.InconsistentMessageException;
import application.domain.exceptions.InsufficientDataException;
import application.domain.exceptions.ReadingAlreadyExistsException;
import application.domain.exceptions.ReadingNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/** Translates business failures into HTTP responses. It is the only place that knows the status codes. */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException exception) {
        return build(statusOf(exception), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(400, message.isBlank() ? "Invalid request." : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        return build(400, "The request body is missing or malformed (check the JSON and the types of the fields).");
    }

    static int statusOf(DomainException exception) {
        if (exception instanceof ReadingNotFoundException) {
            return 404;
        }
        if (exception instanceof ReadingAlreadyExistsException) {
            return 409;
        }
        if (exception instanceof InsufficientDataException || exception instanceof InconsistentMessageException
                || exception instanceof InconsistentDistancesException) {
            return 422;
        }
        return 400; // unknown device, invalid distance/message/information/update
    }

    private ResponseEntity<ErrorResponse> build(int status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status, reason(status), message));
    }

    private String reason(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Content";
            default -> "Error";
        };
    }
}
