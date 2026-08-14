package org.nhindirect.gateway.streams.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.gateway.smtp.MessageProcessResult;
import org.nhindirect.gateway.smtp.SmtpAgent;
import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.gateway.streams.STAPostProcessSource;
import org.nhindirect.gateway.streams.SmtpGatewayMessageSource;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.MessageEnvelope;
import org.nhindirect.stagent.MockNHINDAgent;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.mail.notifications.NotificationMessage;
import org.springframework.messaging.Message;

public class STAProcessor_processMessageTest
{
	private STAProcessor processor;
	private SmtpAgent mockSmtpAgent;
	private SmtpGatewayMessageSource mockSmtpMessageSource;
	private TxDetailParser mockTxParser;
	private TxService mockTxService;
	private DSNCreator mockDsnCreator;
	private STAPostProcessSource mockStaPostProcessSource;

	@BeforeEach
	public void setUp() throws Exception
	{
		mockSmtpAgent = mock(SmtpAgent.class);
		mockSmtpMessageSource = mock(SmtpGatewayMessageSource.class);
		mockTxParser = mock(TxDetailParser.class);
		mockTxService = mock(TxService.class);
		mockDsnCreator = mock(DSNCreator.class);
		mockStaPostProcessSource = mock(STAPostProcessSource.class);

		when(mockSmtpAgent.getAgent()).thenReturn(new MockNHINDAgent(Arrays.asList("cerner.com", "securehealthemail.com")));
		when(mockTxParser.getMessageDetails(any(MimeMessage.class))).thenReturn(new HashMap<>());

		processor = new STAProcessor();
		processor.smtpAgent = mockSmtpAgent;
		processor.smtpMessageSource = mockSmtpMessageSource;
		processor.txParser = mockTxParser;
		processor.txService = mockTxService;
		processor.dsnCreator = mockDsnCreator;
		processor.staPostProcessSource = mockStaPostProcessSource;
		processor.suppressNotificationAddresses = new ArrayList<>();
	}

	/**
	 * Happy path for an incoming message. The agent processes it and returns a single MDN "processed"
	 * notification. With an empty suppress list the MDN must be sent and no DSN must be created.
	 */
	@Test
	public void testProcessIncomingMessage_MDNNotSuppressed_assertMDNSent() throws Exception
	{
		// PlainIncomingMessage.txt: From externUser1@starugh-stateline.com  To user1@cerner.com
		final Message<?> streamMsg = buildIncomingStreamMessage();
		final org.nhindirect.stagent.mail.Message processedMsg = buildMessageFromResource("PlainIncomingMessage.txt");

		// MDN is authored by the domain recipient (From) and addressed back to the original sender (To)
		final NotificationMessage mockMdn = buildMockMdn("user1@cerner.com", "externUser1@starugh-stateline.com");

		final MessageEnvelope mockEnvelope = mock(MessageEnvelope.class);
		when(mockEnvelope.getMessage()).thenReturn(processedMsg);
		when(mockEnvelope.getRejectedRecipients()).thenReturn(null);
		when(mockEnvelope.hasRejectedRecipients()).thenReturn(false);

		when(mockSmtpAgent.processMessage(any(), any(), any()))
				.thenReturn(new MessageProcessResult(mockEnvelope, Collections.singletonList(mockMdn)));

		processor.directStaProcessorInput().accept(streamMsg);

		verify(mockSmtpMessageSource, times(1)).sendMimeMessage(any(MimeMessage.class));
		verify(mockDsnCreator, never()).createDSNFailure(any(), any(), anyBoolean());
	}

	/**
	 * Happy path for an outgoing message where one recipient is rejected. The agent returns a rejected
	 * recipient list, which triggers DSN creation. With an empty suppress list the DSN must be sent.
	 */
	@Test
	public void testProcessOutgoingMessage_DSNNotSuppressed_assertDSNSent() throws Exception
	{
		// PlainOutgoingMessageWithRejectedRecips.txt: From user1@cerner.com
		//   To: externUser1@starugh-stateline.com (trusted), someotherrecip@nontrustedomain.org (rejected)
		final Message<?> streamMsg = buildOutgoingStreamMessage();
		final org.nhindirect.stagent.mail.Message processedMsg =
				buildMessageFromResource("PlainOutgoingMessageWithRejectedRecips.txt");

		final NHINDAddressCollection rejectedRecips = new NHINDAddressCollection();
		rejectedRecips.add(new NHINDAddress("someotherrecip@nontrustedomain.org"));

		final MessageEnvelope mockEnvelope = mock(MessageEnvelope.class);
		when(mockEnvelope.getMessage()).thenReturn(processedMsg);
		when(mockEnvelope.getRejectedRecipients()).thenReturn(rejectedRecips);
		when(mockEnvelope.hasRejectedRecipients()).thenReturn(true);

		when(mockSmtpAgent.processMessage(any(), any(), any()))
				.thenReturn(new MessageProcessResult(mockEnvelope, Collections.emptyList()));

		// DSN is addressed to the original sender
		final MimeMessage dsnMsg = buildDsnMessage("user1@cerner.com");
		when(mockDsnCreator.createDSNFailure(any(), any(), anyBoolean()))
				.thenReturn(Collections.singletonList(dsnMsg));

		processor.directStaProcessorInput().accept(streamMsg);

		verify(mockDsnCreator, times(1)).createDSNFailure(any(), any(), anyBoolean());
		verify(mockSmtpMessageSource, times(1)).sendMimeMessage(any(MimeMessage.class));
	}

	/**
	 * The MDN target recipient (the domain recipient the MDN is generated for, carried in its From
	 * header) appears in suppressNotificationAddresses. The MDN must NOT be sent. Suppression must key
	 * off the recipient, not the original sender who would receive the MDN.
	 */
	@Test
	public void testProcessIncomingMessage_MDNSuppressed_assertMDNNotSent() throws Exception
	{
		processor.suppressNotificationAddresses = Arrays.asList("user1@cerner.com");

		final Message<?> streamMsg = buildIncomingStreamMessage();
		final org.nhindirect.stagent.mail.Message processedMsg = buildMessageFromResource("PlainIncomingMessage.txt");
		final NotificationMessage mockMdn = buildMockMdn("user1@cerner.com", "externUser1@starugh-stateline.com");

		final MessageEnvelope mockEnvelope = mock(MessageEnvelope.class);
		when(mockEnvelope.getMessage()).thenReturn(processedMsg);
		when(mockEnvelope.getRejectedRecipients()).thenReturn(null);
		when(mockEnvelope.hasRejectedRecipients()).thenReturn(false);

		when(mockSmtpAgent.processMessage(any(), any(), any()))
				.thenReturn(new MessageProcessResult(mockEnvelope, Collections.singletonList(mockMdn)));

		processor.directStaProcessorInput().accept(streamMsg);

		verify(mockSmtpMessageSource, never()).sendMimeMessage(any(MimeMessage.class));
	}

	/**
	 * The DSN target recipient (the rejected recipient the DSN concerns) appears in
	 * suppressNotificationAddresses. Suppression must key off the rejected recipient, not the original
	 * sender who would receive the DSN, and must be applied before the DSN is even generated.
	 */
	@Test
	public void testProcessOutgoingMessage_DSNSuppressed_assertDSNNotSent() throws Exception
	{
		processor.suppressNotificationAddresses = Arrays.asList("someotherrecip@nontrustedomain.org");

		final Message<?> streamMsg = buildOutgoingStreamMessage();
		final org.nhindirect.stagent.mail.Message processedMsg =
				buildMessageFromResource("PlainOutgoingMessageWithRejectedRecips.txt");

		final NHINDAddressCollection rejectedRecips = new NHINDAddressCollection();
		rejectedRecips.add(new NHINDAddress("someotherrecip@nontrustedomain.org"));

		final MessageEnvelope mockEnvelope = mock(MessageEnvelope.class);
		when(mockEnvelope.getMessage()).thenReturn(processedMsg);
		when(mockEnvelope.getRejectedRecipients()).thenReturn(rejectedRecips);
		when(mockEnvelope.hasRejectedRecipients()).thenReturn(true);

		when(mockSmtpAgent.processMessage(any(), any(), any()))
				.thenReturn(new MessageProcessResult(mockEnvelope, Collections.emptyList()));

		processor.directStaProcessorInput().accept(streamMsg);

		verify(mockDsnCreator, never()).createDSNFailure(any(), any(), anyBoolean());
		verify(mockSmtpMessageSource, never()).sendMimeMessage(any(MimeMessage.class));
	}

	/**
	 * A plus addressed variant of a configured suppression address (eg. user1+category@cerner.com
	 * when user1@cerner.com is configured) must still be suppressed.
	 */
	@Test
	public void testIsAddressSuppressed_PlusAddressedVariantOfSuppressedAddress_assertSuppressed() throws Exception
	{
		processor.suppressNotificationAddresses = Arrays.asList("user1@cerner.com");

		assertTrue(processor.isAddressSuppressed(new InternetAddress("user1+category@cerner.com")));
	}

	/**
	 * Comparison must remain case insensitive after plus addressing normalization.
	 */
	@Test
	public void testIsAddressSuppressed_PlusAddressedVariantDifferentCase_assertSuppressed() throws Exception
	{
		processor.suppressNotificationAddresses = Arrays.asList("User1@Cerner.com");

		assertTrue(processor.isAddressSuppressed(new InternetAddress("USER1+Category@CERNER.COM")));
	}

	/**
	 * An address that merely shares a local part prefix (not a plus addressing separator) must not
	 * be suppressed.
	 */
	@Test
	public void testIsAddressSuppressed_UnrelatedAddress_assertNotSuppressed() throws Exception
	{
		processor.suppressNotificationAddresses = Arrays.asList("user1@cerner.com");

		assertFalse(processor.isAddressSuppressed(new InternetAddress("user10@cerner.com")));
	}

	// --- helpers ---

	@SuppressWarnings("deprecation")
	private Message<?> buildIncomingStreamMessage() throws Exception
	{
		final String msgText = TestUtils.readMessageResource("PlainIncomingMessage.txt");
		final MimeMessage mimeMsg = new MimeMessage((Session) null, IOUtils.toInputStream(msgText));
		final List<InternetAddress> recipients = Collections.singletonList(new InternetAddress("user1@cerner.com"));
		final InternetAddress sender = new InternetAddress("externUser1@starugh-stateline.com");
		return SMTPMailMessageConverter.toStreamMessage(new SMTPMailMessage(mimeMsg, recipients, sender));
	}

	@SuppressWarnings("deprecation")
	private Message<?> buildOutgoingStreamMessage() throws Exception
	{
		final String msgText = TestUtils.readMessageResource("PlainOutgoingMessageWithRejectedRecips.txt");
		final MimeMessage mimeMsg = new MimeMessage((Session) null, IOUtils.toInputStream(msgText));
		final List<InternetAddress> recipients = Arrays.asList(
				new InternetAddress("externUser1@starugh-stateline.com"),
				new InternetAddress("someotherrecip@nontrustedomain.org"));
		final InternetAddress sender = new InternetAddress("user1@cerner.com");
		return SMTPMailMessageConverter.toStreamMessage(new SMTPMailMessage(mimeMsg, recipients, sender));
	}

	@SuppressWarnings("deprecation")
	private org.nhindirect.stagent.mail.Message buildMessageFromResource(String resourceName) throws Exception
	{
		final String msgText = TestUtils.readMessageResource(resourceName);
		return new org.nhindirect.stagent.mail.Message(
				new MimeMessage((Session) null, IOUtils.toInputStream(msgText)));
	}

	private NotificationMessage buildMockMdn(String fromAddress, String toAddress) throws Exception
	{
		final NotificationMessage mockMdn = mock(NotificationMessage.class);
		when(mockMdn.getFrom()).thenReturn(new Address[] {new InternetAddress(fromAddress)});
		when(mockMdn.getAllRecipients()).thenReturn(new Address[] {new InternetAddress(toAddress)});
		return mockMdn;
	}

	private MimeMessage buildDsnMessage(String toAddress) throws Exception
	{
		final MimeMessage dsnMsg = new MimeMessage((Session) null);
		dsnMsg.setRecipient(RecipientType.TO, new InternetAddress(toAddress));
		dsnMsg.setContent("DSN failure notification", "text/plain");
		return dsnMsg;
	}
}
