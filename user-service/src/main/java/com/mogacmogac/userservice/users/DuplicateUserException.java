package com.mogacmogac.userservice.users;

import com.mogacmogac.userservice.validation.ValidationException;
import org.springframework.validation.FieldError;

public class DuplicateUserException extends ValidationException {

    public DuplicateUserException() {
    }

    public DuplicateUserException(String message, FieldError fieldError) {
        super(message, fieldError);
    }

    public DuplicateUserException(String message, Throwable cause, FieldError fieldError) {
        super(message, cause, fieldError);
    }
}
