package org.nhindirect.gateway.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.Collection;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.nhindirect.common.mail.MDNStandard;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.AddressSource;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.mail.Message;
import org.nhindirect.stagent.mail.notifications.Notification;
import org.nhindirect.stagent.mail.notifications.NotificationMessage;

public class ReliableDispatchedNotificationProducer_produceTest
{


	protected NHINDAddressCollection getMailRecipients(MimeMessage mail) throws MessagingException
	{
		final NHINDAddressCollection recipients = new NHINDAddressCollection();		

		final Address[] recipsAddr = mail.getAllRecipients();
		for (Address addr : recipsAddr)
		{
			
			recipients.add(new NHINDAddress(addr.toString(), (AddressSource)null));
		}

		
		return recipients;
	}
	
	@Test
	public void testCreateAckWithNoText() throws Exception
	{
		final MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("PlainOutgoingMessage.txt"), Charset.defaultCharset()));
		
		final NHINDAddressCollection recipients = getMailRecipients(msg);
				
		final NotificationProducer prod = new ReliableDispatchedNotificationProducer(new NotificationSettings(true, "Local Direct Delivery Agent", ""));
		
		final Collection<NotificationMessage> notifications = 
				prod.produce(new Message(msg), recipients.toInternetAddressCollection());
		
		assertNotNull(notifications);
		
		for (NotificationMessage noteMsg : notifications)
		{
			// assert that we removed the notification option from the headers as part of the fix of 
			// version 1.5.1
			assertNull(noteMsg.getHeader(MDNStandard.Headers.DispositionNotificationOptions, ","));
			final InternetHeaders headers = Notification.getNotificationFieldsAsHeaders(noteMsg);
			assertEquals("", headers.getHeader(MDNStandard.DispositionOption_TimelyAndReliable, ","));
		}
		
	}	
	
}
