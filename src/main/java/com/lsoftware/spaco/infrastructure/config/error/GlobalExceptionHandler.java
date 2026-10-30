package com.lsoftware.spaco.infrastructure.config.error;

import com.lsoftware.spaco.entity.exception.ConflictException;
import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.entity.exception.NotFoundException;
import com.lsoftware.spaco.entity.appointment.exception.TooManyPendingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(DomainException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(DomainException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(TooManyPendingException.class)
    public ResponseEntity<ApiError> tooMany(DomainException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge() {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_IMAGE", "Upload a JPG, PNG or WebP photo of up to 8 MB"));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> domain(DomainException e) {
        return ResponseEntity.badRequest().body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> unreadable() {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Malformed request"));
    }
}
