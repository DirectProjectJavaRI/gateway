package org.nhindirect.gateway.streams.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.smtp.GatewayState;
import org.nhindirect.gateway.smtp.MessageProcessResult;
import org.nhindirect.gateway.smtp.SmtpAgent;
import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.gateway.streams.STAPostProcessSource;
import org.nhindirect.gateway.streams.SmtpGatewayMessageSource;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.mail.notifications.NotificationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class STAProcessor
{	
	@Autowired
	protected SmtpAgent smtpAgent;	
	
	@Autowired
	protected TxDetailParser txParser;
	
	@Autowired
	protected SmtpGatewayMessageSource smtpMessageSource;
	
	@Autowired
	protected TxService txService;
	
	@Autowired
	@Qualifier("rejectedRecipientDSNCreator")
	protected DSNCreator dsnCreator;
	
	@Autowired
	protected STAPostProcessSource staPostProcessSource;

	// Suppression of notifications messages for configured addresses.  Used
	// for testing purposes and various validation tooling scenarios.  Generally not used
	// in a production environment
	@Value("${direct.gateway.notifications.suppressNotificationsForAddresses:}")
	protected List<String> suppressNotificationAddresses;
	
	public STAProcessor()
	{
		
	}
	
	public void setSmtpAgent(SmtpAgent smtpAgent)
	{
		this.smtpAgent = smtpAgent;
	}
	
	public void setSTAPostProcessSource(STAPostProcessSource staPostProcessSource)
	{
		this.staPostProcessSource = staPostProcessSource;
	}	
	
	@Bean
	public Consumer<Message<?>> directStaProcessorInput()
	{
		return streamMsg ->
		{
					
			MessageProcessResult result = null;
			
			Tx txToMonitor = null;	
			
			SMTPMailMessage smtpMessage = SMTPMailMessageConverter.fromStreamMessage(streamMsg);
			
			boolean isOutgoing = false;
			GatewayState.getInstance().lockForProcessing();
			try
			{
				final NHINDAddressCollection recipients = MessageUtils.getMailRecipients(smtpMessage);
				
				final NHINDAddress sender = MessageUtils.getMailSender(smtpMessage);
				
				isOutgoing = MessageUtils.isOutgoing(smtpMessage.getMimeMessage(), sender, smtpAgent.getAgent());
				
				// if the message is outgoing, then the tracking information must be
				// gathered now before the message is transformed
				if (isOutgoing)
					txToMonitor = MessageUtils.getTxToTrack(smtpMessage.getMimeMessage(), sender, recipients, this.txParser);
				
				// recipients can get modified by the security and trust agent, so make a local copy
				// before processing
				final NHINDAddressCollection originalRecipList = NHINDAddressCollection.create(recipients);
				
				try
				{
					// process the message with the agent stack
					log.trace("Calling stapProcessor.processSmtpMessage");
					result = smtpAgent.processMessage(smtpMessage.getMimeMessage(), recipients, sender);
					log.trace("Finished calling agent.processMessage");
					
					if (result == null)
					{				
						log.error("Failed to process message.  processMessage returned null.");		
						
						onMessageRejected(smtpMessage, originalRecipList, sender, isOutgoing, txToMonitor, null);
						
						
						log.trace("Exiting service(Mail mail)");
						return;
					}
				}	
				catch (Throwable e)
				{
					// catch all
					
					log.info("Failed to process message: " + e.getMessage(), e);					
					
					onMessageRejected(smtpMessage, originalRecipList, sender, isOutgoing, txToMonitor, e);
					
					return;
				}
				
				log.debug("Updating SMTPMailMessage message with processed result");
				if (result.getProcessedMessage() != null)
				{
					smtpMessage = new SMTPMailMessage((MimeMessage)result.getProcessedMessage().getMessage(), 
							(List<InternetAddress>)recipients.toInternetAddressCollection(), 
							(InternetAddress)sender);
				}
				else
				{
		
					log.debug("Processed message is null.  Eat the message.");
		
					return;
				}
				
				log.trace("Removing reject recipients from the RCTP headers");
				// remove reject recipients from the RCTP headers
				if (result.getProcessedMessage().getRejectedRecipients() != null && 
						result.getProcessedMessage().getRejectedRecipients().size() > 0 && smtpMessage.getRecipientAddresses() != null &&
								smtpMessage.getRecipientAddresses().size() > 0)
				{
					
					final List<InternetAddress> newRCPTList = new ArrayList<InternetAddress>();
					for (InternetAddress rctpAdd : smtpMessage.getRecipientAddresses())
					{
						if (!MessageUtils.isRcptRejected(rctpAdd, result.getProcessedMessage().getRejectedRecipients()))
						{
							newRCPTList.add(rctpAdd);
						}
					}
					
					smtpMessage = new SMTPMailMessage(smtpMessage.getMimeMessage(), newRCPTList, (InternetAddress)sender);
				}
				
				log.trace("Handling sending MDN messages");
				/*
				 * Handle sending MDN messages
				 */
				final Collection<NotificationMessage> notifications = result.getNotificationMessages();
				if (notifications != null && notifications.size() > 0)
				{
					log.trace("MDN messages requested.  Sending MDN \"processed\" messages");
					// create a message for each notification and send it the SmtpGatewayMessageProcessor via streams
					for (NotificationMessage message : notifications)
					{
						try
						{
							if (!isNotificationSuppressed(message))
								smtpMessageSource.sendMimeMessage(message);
						}
						catch (Throwable t)
						{
							// don't kill the process if this fails
							log.error("Error sending MDN message.", t);
						}
					}
				}
				
			}
			catch (MessagingException e)
			{
				throw new RuntimeException(e);
			}
			finally
			{
				GatewayState.getInstance().unlockFromProcessing();
			}

			// These operations do not reference the agent and do not need the processing lock.
			// Releasing the lock before these calls prevents long-held read locks from blocking
			// the SettingsManager write lock and starving other consumers.
			log.trace("Track message");
			MessageUtils.trackMessage(txToMonitor, isOutgoing, txService);

			log.trace("Post processing for rejected recips.");
			onPostprocessMessage(smtpMessage, result, isOutgoing, txToMonitor);

			log.trace("Sending to sta post process");
			staPostProcessSource.staPostProcess(smtpMessage);
			
			log.trace("Exiting Message<?> streamMsg");
		};
	}
	
	protected void onMessageRejected(SMTPMailMessage mail, NHINDAddressCollection recipients, NHINDAddress sender, boolean isOutgoing,
			Tx tx, Throwable t)
	{
		// if this is an outgoing IMF message, then we need to send a DSN message
		if (isOutgoing && tx != null && tx.getMsgType() == TxMessageType.IMF)
		{
			log.debug("Sending DSN message due to rejected message");
			sendDSN(tx, recipients, true);
		}
	}
	
	
	protected void onPostprocessMessage(SMTPMailMessage mail, MessageProcessResult result, boolean isOutgoing, Tx tx)
	{
		// if there are rejected recipients and an outgoing IMF message, then we need to send a DSN message
		if (isOutgoing && tx != null && tx.getMsgType() == TxMessageType.IMF && result.getProcessedMessage().hasRejectedRecipients())
		{
			log.debug("Sending DSN message due to rejected recipients");
			sendDSN(tx, result.getProcessedMessage().getRejectedRecipients(), true);
		}

	}	
	
	/*
	 * Tests if the given address matches an address in the configured suppression list.
	 */
	protected boolean isAddressSuppressed(InternetAddress address)
	{
		if (suppressNotificationAddresses == null || suppressNotificationAddresses.isEmpty() || address == null)
			return false;

		final String emailAddr = normalizeAddress(address.getAddress());
		if (emailAddr == null)
			return false;

		for (String suppressAddr : suppressNotificationAddresses)
		{
			if (!suppressAddr.trim().isEmpty() && emailAddr.equalsIgnoreCase(suppressAddr.trim()))
				return true;
		}

		return false;
	}

	/*
	 * Normalizes a message address for suppression comparison by lower casing it and stripping
	 * any plus addressing tag (eg. gm2552+category@example.com becomes gm2552@example.com) from
	 * the local part, so that plus addressed variants of a configured suppression address are
	 * also suppressed. Configured suppression addresses are not plus-addressing normalized since
	 * they are expected to already be canonical addresses.
	 */
	protected String normalizeAddress(String address)
	{
		if (address == null)
			return null;

		final String trimmedAddr = address.trim();
		if (trimmedAddr.isEmpty())
			return null;

		final int atIdx = trimmedAddr.indexOf('@');
		if (atIdx < 0)
			return trimmedAddr.toLowerCase();

		String localPart = trimmedAddr.substring(0, atIdx);
		final String domainPart = trimmedAddr.substring(atIdx);

		final int plusIdx = localPart.indexOf('+');
		if (plusIdx >= 0)
			localPart = localPart.substring(0, plusIdx);

		return (localPart + domainPart).toLowerCase();
	}

	/*
	 * MDN "processed" notifications are authored by the recipient they concern (carried in the
	 * message's From header) and addressed back to the original sender, so suppression must be
	 * decided against the From address, not the message's own recipients.
	 */
	protected boolean isNotificationSuppressed(MimeMessage message)
	{
		if (suppressNotificationAddresses == null || suppressNotificationAddresses.isEmpty())
			return false;

		try
		{
			final Address[] fromAddrs = message.getFrom();
			if (fromAddrs != null)
			{
				for (Address addr : fromAddrs)
				{
					if (addr instanceof InternetAddress && isAddressSuppressed((InternetAddress) addr))
						return true;
				}
			}
		}
		catch (MessagingException e)
		{
			log.warn("Could not read From address to check notification suppression", e);
		}
		return false;
	}

	protected void sendDSN(Tx tx, NHINDAddressCollection undeliveredRecipeints, boolean useSenderAsPostmaster)
	{
		try
		{
			// A generated DSN is always addressed back to the original sender, so suppression has to be
			// decided against the failed/rejected recipients themselves (the addresses this list is meant
			// to target) before the DSN is generated, not against the resulting DSN message's own headers.
			final NHINDAddressCollection notSuppressedRecipients = new NHINDAddressCollection();
			for (NHINDAddress recip : undeliveredRecipeints)
			{
				if (!isAddressSuppressed(recip))
					notSuppressedRecipients.add(recip);
			}

			if (notSuppressedRecipients.isEmpty())
			{
				log.debug("All undelivered recipients are configured suppressed addresses; not generating a DSN notification");
				return;
			}

			final Collection<MimeMessage> msgs = dsnCreator.createDSNFailure(tx, notSuppressedRecipients, useSenderAsPostmaster);
			if (msgs != null && msgs.size() > 0)
				for (MimeMessage msg : msgs)
					smtpMessageSource.sendMimeMessage(msg);
		}
		catch (Throwable e)
		{
			// don't kill the process if this fails
			log.error("Error sending DSN failure message.", e);
		}
	}
}
