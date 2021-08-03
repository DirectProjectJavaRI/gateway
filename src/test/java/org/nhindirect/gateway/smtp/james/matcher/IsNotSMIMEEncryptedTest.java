package org.nhindirect.gateway.smtp.james.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.james.core.MailAddress;
import org.nhindirect.gateway.smtp.james.mailet.MockMail;
import org.nhindirect.gateway.testutils.TestUtils;

public class IsNotSMIMEEncryptedTest 
{
	@Test
	public void testIsNotSMIMEMessage_unecryptedMessage_assertAllRecips() throws Exception
	{
		MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("PlainOutgoingMessage.txt"), Charset.defaultCharset()));
		
		
		IsNotSMIMEEncrypted matcher = new IsNotSMIMEEncrypted();
		
		final Collection<MailAddress> initialRecips = new ArrayList<MailAddress>();
		for (Address addr : msg.getAllRecipients())
			initialRecips.add(new MailAddress(((InternetAddress)addr).getAddress()));
		
		final MockMail mockMail = new MockMail(msg);
		mockMail.setRecipients(initialRecips);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(1, matchAddresses.size());
		assertEquals(initialRecips.iterator().next().toString(), matchAddresses.iterator().next().toString());
	}
	
	@Test
	public void testIsNotSMIMEMessage_ecryptedMessage_assertNull() throws Exception
	{
		MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("EncryptedMessage.txt"), Charset.defaultCharset()));
		
		
		IsNotSMIMEEncrypted matcher = new IsNotSMIMEEncrypted();
		
		final Collection<MailAddress> initialRecips = new ArrayList<MailAddress>();
		for (Address addr : (Address[])msg.getAllRecipients())
			initialRecips.add(new MailAddress(((InternetAddress)addr).getAddress()));
		
		final MockMail mockMail = new MockMail(msg);
		mockMail.setRecipients(initialRecips);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(null, matchAddresses);
	}
	
	@Test
	public void testIsNotSMIMEMessage_nullMail_assertNull() throws Exception
	{
		
		IsNotSMIMEEncrypted matcher = new IsNotSMIMEEncrypted();
		
		
		Collection<MailAddress> matchAddresses = matcher.match(null);
		
		assertEquals(null, matchAddresses);
	}
	
	@Test
	public void testIsNotSMIMEMessage_nullMessage_assertNull() throws Exception
	{
		
		IsNotSMIMEEncrypted matcher = new IsNotSMIMEEncrypted();
		final MockMail mockMail = new MockMail(null);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(null, matchAddresses);
	}
}
