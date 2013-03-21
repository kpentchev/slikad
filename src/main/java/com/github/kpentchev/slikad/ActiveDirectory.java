package com.github.kpentchev.slikad;

import java.util.Properties;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author konstantin.pentchev
 *
 */
public class ActiveDirectory {

	private static final String CONTEXT_FACTORY_CLASS = "com.sun.jndi.ldap.LdapCtxFactory";

	private DirContext dirContext;

	private final String domainController;

	private final Properties properties;

	private final SearchControls searchCtrls;

	private String[] returnAttributes = { "givenName", "mail", "displayName",
			"samaccountname" };

	private Logger logger = LoggerFactory.getLogger(ActiveDirectory.class);

	private String filterFormat = "(&(objectCategory=Person)(objectClass=User)(|(samaccountname=%$1s)(givenName=%$1s))";

	public ActiveDirectory(String domainController) {
		this(domainController, null, null);
	}

	public ActiveDirectory(String domainController, String username,
			String password) {
		this.domainController = domainController;
		this.properties = new Properties();
		this.properties.put(Context.INITIAL_CONTEXT_FACTORY,
				CONTEXT_FACTORY_CLASS);
		this.properties.put(Context.PROVIDER_URL, "LDAP://"
				+ this.domainController);
		if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
			this.properties.put(Context.SECURITY_PRINCIPAL, username);
			this.properties.put(Context.SECURITY_CREDENTIALS, password);
		}
		connect();
		searchCtrls = new SearchControls();
		searchCtrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		searchCtrls.setReturningAttributes(returnAttributes);
	}

	private void connect() {
		try {
			dirContext = new InitialDirContext(properties);
		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Frees-up resources.
	 */
	public void close(){
		try {
			this.dirContext.close();
		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Retrieves detail information about an account from the AD.
	 * 
	 * @param accountName
	 *            the windows user account name (without domain prefix),
	 * @return {@link UserDetails}.
	 */
	public UserDetails getUserDetails(String accountName) {
		UserDetails userDetails = new UserDetails(accountName);
		try {
			NamingEnumeration<SearchResult> results = dirContext.search(
					getDomainBase(domainController), getFilter(accountName),
					searchCtrls);
			boolean hasResults = results.hasMoreElements();
			if (hasResults) {
				SearchResult result = results.nextElement();
				Attributes attrs = result.getAttributes();
				Attribute name = attrs.get("displayName");
				if (name != null) {
					userDetails.setName((String) name.get());
				}
				Attribute mail = attrs.get("mail");
				if (mail != null) {
					userDetails.setMail((String) mail.get());
				}
			}
		} catch (CommunicationException e) {
			logger.info("Reconnecting to AD...");
			connect();
			return getUserDetails(accountName);
		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
		}
		return userDetails;
	}

	private String getFilter(String searchValue) {
		return String.format(filterFormat, searchValue);
	}

	private static String getDomainBase(String base) {
		String[] parts = base.split("\\.");
		StringBuilder builder = new StringBuilder();
		builder.append("DC=");
		builder.append(StringUtils.join(parts, ",DC="));
		return builder.toString();
	}

}
