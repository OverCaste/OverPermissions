package com.overmc.overpermissions;

public class StartException extends RuntimeException {
	private static final long serialVersionUID = 5317781735534101801L;
	private final String simpleMessage;

	public StartException(String simpleMessage) {
		super();
		this.simpleMessage = simpleMessage;
	}

	public StartException(String message, String simpleMessage) {
		super(message);
		this.simpleMessage = simpleMessage;
	}

	public String getSimpleMessage( ) {
		return simpleMessage;
	}
}
