package me.tgoyal.clearance.web;

import jakarta.servlet.http.HttpServletRequest;
import me.tgoyal.clearance.domain.AlreadyDecidedException;
import me.tgoyal.clearance.parser.DependencyFileException;
import me.tgoyal.clearance.service.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Turns exceptions from any controller into the one {@link ErrorResponse} JSON shape.
 * Controllers and services just throw; the mapping to status codes lives only here.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(AlreadyDecidedException.class)
    ResponseEntity<ErrorResponse> alreadyDecided(AlreadyDecidedException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    /** Two reviewers decided the same component at the same moment; the second one loses. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> concurrentChange(ObjectOptimisticLockingFailureException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "Someone else changed this component at the same time. Reload and try again.", request);
    }

    @ExceptionHandler(DependencyFileException.class)
    ResponseEntity<ErrorResponse> badFile(DependencyFileException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    /** A @Valid request body failed its checks. Lists every bad field, e.g. "comment: must not be blank". */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::describe)
                .sorted()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    /** A constraint on a method parameter failed, e.g. a blank {@code projectName}. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> invalidParameter(HandlerMethodValidationException e, HttpServletRequest request) {
        String message = e.getParameterValidationResults().stream()
                .map(result -> result.getMethodParameter().getParameterName() + ": "
                        + result.getResolvableErrors().get(0).getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErrorResponse> missingPart(MissingServletRequestPartException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Missing upload field '" + e.getRequestPartName() + "'", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Missing parameter '" + e.getParameterName() + "'", request);
    }

    /** E.g. {@code ?status=MAYBE}, or a non-numeric id. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> wrongType(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value '" + e.getValue() + "' for '" + e.getName() + "'", request);
    }

    /** Body is not valid JSON, or has a value of the wrong type (e.g. an unknown decision). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is not valid JSON for this endpoint", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "File is larger than the 2 MB limit", request);
    }

    /**
     * Anything else is a bug. Log it with the stack trace (never swallow it), but don't
     * send internal details to the client.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request);
    }

    private static String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
