package org.nhindirect.gateway.smtp.james.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

import org.apache.james.core.MailAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.MatcherConfig;
import org.nhindirect.gateway.smtp.SmtpAgentException;

public class RecipAndSenderIsNotLocalTest
{
	@Test
	public void testNullDomainList() throws Exception
	{
		final MatcherConfig newConfig = mock(MatcherConfig.class);
		when(newConfig.getCondition()).thenReturn(null);
		
		RecipAndSenderIsNotLocal matcher = new RecipAndSenderIsNotLocal();
		
		boolean exceptionOccured = false;
		
		try
		{
			matcher.init(newConfig);
		}
		catch (SmtpAgentException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}
	
	@Test
	public void testEmptyDomainList() throws Exception
	{
		final MatcherConfig newConfig = mock(MatcherConfig.class);
		when(newConfig.getCondition()).thenReturn("");
		
		RecipAndSenderIsNotLocal matcher = new RecipAndSenderIsNotLocal();
		
		boolean exceptionOccured = false;
		
		try
		{
			matcher.init(newConfig);
		}
		catch (SmtpAgentException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testMatch_RemoteSender_AssertRecipeintReturned() throws Exception
	{
		final Mail mockMail = mock(Mail.class);
		when(mockMail.getSender()).thenReturn(new MailAddress("me@remoteMail.com"));
		when(mockMail.getRecipients()).thenReturn(Arrays.asList(new MailAddress("you@cerner.com")));
		
		final MatcherConfig newConfig = mock(MatcherConfig.class);
		when(newConfig.getCondition()).thenReturn("cerner.com");
		
		RecipAndSenderIsNotLocal matcher = new RecipAndSenderIsNotLocal();
		matcher.init(newConfig);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(1, matchAddresses.size());
		assertEquals("you@cerner.com", matchAddresses.iterator().next().toString());
	}	
	
	@Test
	@SuppressWarnings("unchecked")
	public void testMatch_LocalSender_RemoteRcpt_AssertRecipeintReturned() throws Exception
	{
		final Mail mockMail = mock(Mail.class);
		when(mockMail.getSender()).thenReturn(new MailAddress("me@cerner.com"));
		when(mockMail.getRecipients()).thenReturn(Arrays.asList(new MailAddress("you@remoteMail")));
		
		final MatcherConfig newConfig = mock(MatcherConfig.class);
		when(newConfig.getCondition()).thenReturn("cerner.com");
		
		RecipAndSenderIsNotLocal matcher = new RecipAndSenderIsNotLocal();
		matcher.init(newConfig);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(1, matchAddresses.size());
		assertEquals("you@remotemail", matchAddresses.iterator().next().toString());
	}	
	
	@Test
	@SuppressWarnings("unchecked")
	public void testMatch_LocalSender_LocalRcpt_AssertNoneReturned() throws Exception
	{
		final Mail mockMail = mock(Mail.class);
		when(mockMail.getSender()).thenReturn(new MailAddress("me@cerner.com"));
		when(mockMail.getRecipients()).thenReturn(Arrays.asList(new MailAddress("you@cerner.com")));
		
		final MatcherConfig newConfig = mock(MatcherConfig.class);
		when(newConfig.getCondition()).thenReturn("cerner.com");
		
		RecipAndSenderIsNotLocal matcher = new RecipAndSenderIsNotLocal();
		matcher.init(newConfig);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(0, matchAddresses.size());
	}	
	
	@Test
	@SuppressWarnings("unchecked")
	public void testMatch_LocalSender_LocalAndRemoteRcpt_AssertRemoteRcptReturned() throws Exception
	{
		final Mail mockMail = mock(Mail.class);
		when(mockMail.getSender()).thenReturn(new MailAddress("me@cerner.com"));
		when(mockMail.getRecipients()).thenReturn(Arrays.asList(new MailAddress("you@cerner.com"), new MailAddress("someone@remoteMail.com")));
		
		final MatcherConfig newConfig = mock(MatcherConfig.class);
		when(newConfig.getCondition()).thenReturn("cerner.com");
		
		RecipAndSenderIsNotLocal matcher = new RecipAndSenderIsNotLocal();
		matcher.init(newConfig);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(1, matchAddresses.size());
		assertEquals("someone@remotemail.com", matchAddresses.iterator().next().toString());
	}		
}
