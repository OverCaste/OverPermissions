package com.overmc.overpermissions.exceptions;

public class PlayerNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 5960285004982766288L;

	public PlayerNotFoundException(String message) {
		super(message);
	}
}
