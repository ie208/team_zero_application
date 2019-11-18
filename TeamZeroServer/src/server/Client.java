package server;
/**
 * 
 * Encapsulates a single online chat Client
 *
 */
public class Client {
	private String username;
	private String email;
	private int id;
	private boolean isLoggedIn;
	

	// TODO add an image object for profile pics 
	public Client(String username, String email, int id, boolean isLoggedIn) {
		this.username = username;
		this.email = email;
		this.id = id;
		this.isLoggedIn = isLoggedIn;
	}
	
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}


	public boolean isLoggedIn() {
		return isLoggedIn;
	}


	public void setLoggedIn(boolean isLoggedIn) {
		this.isLoggedIn = isLoggedIn;
	}

}
