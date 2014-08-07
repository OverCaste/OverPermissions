package com.overmc.overpermissions.exceptions;

public class TimeFormatException extends RuntimeException {
    private static final long serialVersionUID = 8307489484891709887L;

    public TimeFormatException( ) {
        super();
    }

    public TimeFormatException(String message) {
        super(message);
    }

    public TimeFormatException(Throwable cause) {
        super(cause);
    }

    public TimeFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
