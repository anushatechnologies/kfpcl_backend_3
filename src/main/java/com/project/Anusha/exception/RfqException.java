package com.project.Anusha.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RfqException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public RfqException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public static RfqException notFound(String message) {
        return new RfqException(message, "RFQ_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static RfqException productNotFound(String message) {
        return new RfqException(message, "PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static RfqException accessDenied(String message) {
        return new RfqException(message, "RFQ_ACCESS_DENIED", HttpStatus.FORBIDDEN);
    }

    public static RfqException alreadyAccepted(String message) {
        return new RfqException(message, "RFQ_ALREADY_ACCEPTED", HttpStatus.CONFLICT);
    }

    public static RfqException alreadyRejected(String message) {
        return new RfqException(message, "RFQ_ALREADY_REJECTED", HttpStatus.CONFLICT);
    }

    public static RfqException notResponded(String message) {
        return new RfqException(message, "RFQ_NOT_RESPONDED", HttpStatus.BAD_REQUEST);
    }

    public static RfqException invalidStatus(String message) {
        return new RfqException(message, "INVALID_RFQ_STATUS", HttpStatus.BAD_REQUEST);
    }

    public static RfqException contactNotAvailable(String message) {
        return new RfqException(message, "CONTACT_NOT_AVAILABLE", HttpStatus.FORBIDDEN);
    }

    public static RfqException invalidRequest(String message) {
        return new RfqException(message, "INVALID_REQUEST", HttpStatus.BAD_REQUEST);
    }
}
