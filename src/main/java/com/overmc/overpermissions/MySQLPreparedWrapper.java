package com.overmc.overpermissions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * A resource closing wrapper for JDBC queries. It will close any resources opened by the {@link createPreparedStatement(String)}, {@link createStatement(String)}, {@link createResultSet(Statement, String)}, and {@link createResultSet(PreparedStatement)} methods. <br>
 * <br>
 * Example code:
 * 
 * <pre>
 * {@code
 * return (new MySQLPreparedWrapper<Boolean>(this) {
 *  public Boolean execute( ) throws SQLException {
 *   int permissionId = getPermissionValue(permission, false);
 *   PreparedStatement pst = createPreparedStatement("SELECT EXISTS(SELECT 1 FROM Player_Permission WHERE player_uid=? AND world_uid=? AND permission_uid=? LIMIT 1)");
 *   pst.setInt(1, playerId);
 *   pst.setInt(2, worldId);
 *   pst.setInt(3, permissionId);
 *   ResultSet rs = createResultSet(pst);
 *   if (rs.next()) {
 *    return rs.getBoolean(1);
 *   }
 *   return false;
 *  }
 * }).call(false);
 * }
 * </pre>
 * 
 * @author <a href="http://www.reddit.com/user/TheOverCaste/">OverCaste</a>
 * 
 * @param <T> The return value of this wrapper.
 * @see {@link MySQLManager} and {@link SQLManager} for more information.
 */
public abstract class MySQLPreparedWrapper<T> {
	private final MySQLManager sqlManager;

	protected final ArrayList<ResultSet> resultSets = new ArrayList<ResultSet>(3);
	protected final ArrayList<PreparedStatement> preparedStatements = new ArrayList<PreparedStatement>(3);
	protected final ArrayList<Statement> statements = new ArrayList<Statement>(3);

	public MySQLPreparedWrapper(MySQLManager sqlManager) {
		this.sqlManager = sqlManager;
	}

	public abstract T execute( ) throws SQLException;

	/**
	 * Attempts to run the execute method, close all opened JDBC handles, then return it's value. If an exception is thrown, it is caught, and the default value is returned instead.
	 * 
	 * @param defaultValue The value to be returned in case of exception.
	 * @return The value specified by the {@link execute()} method. If an exception is thrown, the value passed as an argument.
	 */
	public T call(T defaultValue) {
		try {
			return execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			for (ResultSet rs : this.resultSets) {
				try {
					if ((rs != null) && !rs.isClosed()) {
						rs.close();
					}
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			for (Statement st : this.statements) {
				try {
					if ((st != null) && !st.isClosed()) {
						st.close();
					}
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			for (PreparedStatement pst : this.preparedStatements) {
				try {
					if ((pst != null) && !pst.isClosed()) {
						pst.close();
					}
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
		}
		return defaultValue;
	}

	protected PreparedStatement createPreparedStatement(String statement) throws SQLException {
		PreparedStatement pst = this.sqlManager.getConnection().prepareStatement(statement);
		this.preparedStatements.add(pst);
		return pst;
	}

	protected PreparedStatement createPreparedStatement(String statement, int returnGeneratedKeys) throws SQLException {
		PreparedStatement pst = this.sqlManager.getConnection().prepareStatement(statement, returnGeneratedKeys);
		this.preparedStatements.add(pst);
		return pst;
	}

	protected Statement createStatement( ) throws SQLException {
		Statement st = this.sqlManager.getConnection().createStatement();
		this.statements.add(st);
		return st;
	}

	protected ResultSet createResultSet(Statement st, String query) throws SQLException {
		ResultSet rs = st.executeQuery(query);
		this.resultSets.add(rs);
		return rs;
	}

	protected ResultSet createResultSet(PreparedStatement st) throws SQLException {
		ResultSet rs = st.executeQuery();
		this.resultSets.add(rs);
		return rs;
	}

	protected ResultSet getGeneratedKeys(Statement st) throws SQLException {
		ResultSet rs = st.getGeneratedKeys();
		this.resultSets.add(rs);
		return rs;
	}

	protected int getGeneratedKey(Statement st) throws SQLException {
		ResultSet generatedKeys = getGeneratedKeys(st);
		if (generatedKeys.next()) {
			return generatedKeys.getInt(1);
		}
		throw new RuntimeException("An invalid key was requested! This should never happen!");
	}
}
