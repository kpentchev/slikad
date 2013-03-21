package com.github.kpentchev.slikad;

/**
 * Class holding details about a user from the AD.
 * 
 * @author konstantin.pentchev
 *
 */
public class UserDetails {
	private String name;
	private final String account;
	private String mail;
	
	public UserDetails(String accountName){
		this.account = accountName;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getAccount() {
		return account;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}
	
	

}
