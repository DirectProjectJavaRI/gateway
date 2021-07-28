package org.nhindirect.gateway.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javax.mail.internet.InternetAddress;

public class DomainPostmasterTest 
{
	@Test
	public void testConstructDefaultPostmaster()
	{
		DomainPostmaster postmaster = new DomainPostmaster();
		
		assertNotNull(postmaster.getPostmaster());
		assertEquals("", postmaster.getDomain());
	}
	
	@Test
	public void testConstructPostmasterWithDomain()
	{
		DomainPostmaster postmaster = new DomainPostmaster("domain1", null);
		
		assertNotNull(postmaster.getPostmaster());
		assertNotNull(postmaster.getPostmaster().toString());
		assertEquals("postmaster@domain1", postmaster.getPostmaster().getAddress());
		assertEquals("domain1", postmaster.getDomain());
	}	
	
	@Test
	public void testConstructPostmasterWithDomainAndPostmaster() throws Exception
	{
		DomainPostmaster postmaster = new DomainPostmaster("domain1", new InternetAddress("me@domain1"));
		
		assertNotNull(postmaster.getPostmaster());
		assertEquals("me@domain1", postmaster.getPostmaster().toString());
		assertEquals("domain1", postmaster.getDomain());
	}	
	
	@Test
	public void testConstructPostmaster_NullDomain_AssertException() throws Exception
	{
		boolean exceptionOccured = false;
		try
		{
			new DomainPostmaster(null, null);
		}
		catch (IllegalArgumentException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}	
	
	@Test
	public void testSetDomain_DefaultConstructor() throws Exception
	{
		DomainPostmaster postmaster = new DomainPostmaster();
		
		postmaster.setDomain("domain2");
		
		assertEquals("domain2", postmaster.getDomain());
	}	
	
	@Test
	public void testSetDomain_ParamConstructor() throws Exception
	{
		DomainPostmaster postmaster = new DomainPostmaster("domain1", new InternetAddress("me@domain1"));
		
		postmaster.setDomain("domain2");
		assertEquals("domain2", postmaster.getDomain());
	}
	
	@Test
	public void testSetPostmaster_DefaultConstructor() throws Exception
	{
		DomainPostmaster postmaster = new DomainPostmaster();
		
		postmaster.setPostmasters(new InternetAddress("me@domain1"));
		assertEquals("me@domain1", postmaster.getPostmaster().toString());
	}	
	
	@Test
	public void testSetPostmaster_ParamConstructor() throws Exception
	{
		DomainPostmaster postmaster = new DomainPostmaster("domain1", new InternetAddress("me@domain1"));
			
		postmaster.setPostmasters(new InternetAddress("me@domain2"));
		assertEquals("me@domain2", postmaster.getPostmaster().toString());
	}		
}

