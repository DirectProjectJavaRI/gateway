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

public class IsNotificationTest
{
	@Test
	public void testIsNotification_MDNMessage_assertAllRecips() throws Exception
	{
		MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("MDNMessage.txt"), Charset.defaultCharset()));
		
		
		IsNotification matcher = new IsNotification();
		
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
	public void testIsNotification_DSNMessage_assertAllRecips() throws Exception
	{
		MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("DSNMessage.txt"), Charset.defaultCharset()));
		
		
		IsNotification matcher = new IsNotification();
		
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
	public void testIsNotification_ecryptedMessage_assertNull() throws Exception
	{
		MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("EncryptedMessage.txt"), Charset.defaultCharset()));
		
		
		IsNotSMIMEEncrypted matcher = new IsNotSMIMEEncrypted();
		
		final Collection<MailAddress> initialRecips = new ArrayList<MailAddress>();
		for (Address addr : msg.getAllRecipients())
			initialRecips.add(new MailAddress(((InternetAddress)addr).getAddress()));
		
		final MockMail mockMail = new MockMail(msg);
		mockMail.setRecipients(initialRecips);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(null, matchAddresses);
	}

	@Test
	public void testIsNotification_plainMessage_assertNull() throws Exception
	{
		MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("PlainOutgoingMessage.txt"),Charset.defaultCharset()));
		
		
		IsNotification matcher = new IsNotification();
		
		final Collection<MailAddress> initialRecips = new ArrayList<MailAddress>();
		for (Address addr : msg.getAllRecipients())
			initialRecips.add(new MailAddress(((InternetAddress)addr).getAddress()));
		
		final MockMail mockMail = new MockMail(msg);
		mockMail.setRecipients(initialRecips);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(null, matchAddresses);
	}
	
	@Test
	public void testIsNoticiation_nullMail_assertNull() throws Exception
	{
		
		IsNotification matcher = new IsNotification();
		
		
		Collection<MailAddress> matchAddresses = matcher.match(null);
		
		assertEquals(null, matchAddresses);
	}
	
	@Test
	public void testIsNotification_nullMessage_assertNull() throws Exception
	{
		
		IsNotification matcher = new IsNotification();
		final MockMail mockMail = new MockMail(null);
		
		Collection<MailAddress> matchAddresses = matcher.match(mockMail);
		
		assertEquals(null, matchAddresses);
	}
	
}