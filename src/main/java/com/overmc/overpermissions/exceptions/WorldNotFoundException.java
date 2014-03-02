package com.overmc.overpermissions.exceptions;

public class WorldNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 5960285004982766288L;

	public WorldNotFoundException(String message) {
		super(message);
	}
}
