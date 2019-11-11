package server;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A class to connect to and perform queries on the database
 *
 */
public class DbConnection {

	private final String url = "jdbc:postgresql://localhost:5432/testdb";
	private final String user = "postgres";
	private final String password = "123456";

	/**
	 * Connect to the PostgreSQL database
	 *
	 * @return a Connection object
	 */
	public Connection connect() {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, user, password);
			System.out.println("Connected to the PostgreSQL server successfully.");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return conn;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		DbConnection dbConnection = new DbConnection();
		dbConnection.connect();
	}

	public void addUser(String userName, String password, String email) {
		DbConnection dbConnection = new DbConnection();
		Connection conn = dbConnection.connect();
		PreparedStatement ps = null;
		try {
			ps=conn.prepareStatement("SELECT * FROM USERS WHERE username = ? OR email = ?;");
			ps.setString(1, userName);
			ps.setString(2, email);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String existingUser = rs.getString("username");
				String existingEmail = rs.getString("email");
				// TODO: Send errors to frontend
				if (existingUser.equals(userName)) {
					System.out.println("Username already exists");
				} else if (existingEmail.equals(email)) {
					System.out.println("Email already exists");
				}
			} else {
				ps = conn.prepareStatement("INSERT INTO USERS (username,password,email) VALUES (?, ?, ?)");
				ps.setString(1, userName);
				ps.setString(2, getMd5(password));
				ps.setString(3, email);
				ps.executeUpdate();
				System.out.println("Added user.");
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private String getMd5(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger no = new BigInteger(1, messageDigest);
			String hashtext = no.toString(16);
			while (hashtext.length() < 32) {
				hashtext = "0" + hashtext;
			}
			return hashtext;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}