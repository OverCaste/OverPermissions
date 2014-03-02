package com.overmc.overpermissions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public abstract class SimpleJDBCSQLManager implements SQLManager {
	protected final String dbUrl;
	protected final String dbName;
	protected final String dbUsername;
	protected final String dbPassword;

	public SimpleJDBCSQLManager(String dbUrl, String dbName, String dbUsername, String dbPassword) {
		this.dbUrl = dbUrl;
		this.dbName = dbName;
		this.dbUsername = dbUsername;
		this.dbPassword = dbPassword;
	}

	public static void attemptClose(PreparedStatement pst) {
		try {
			if (pst != null) {
				pst.close();
			}
		} catch (Throwable t) {
		}
	}

	public static void attemptClose(Statement st) {
		try {
			if (st != null) {
				st.close();
			}
		} catch (Throwable t) {
		}
	}

	public static void attemptClose(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (Throwable t) {
		}
	}
}
