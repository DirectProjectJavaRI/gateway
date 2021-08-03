package org.nhindirect.gateway.streams.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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
							smtpMessageSource.sendMimeMessage(message);
						}
						catch (Throwable t)
						{
							// don't kill the process if this fails
							log.error("Error sending MDN message.", t);
						}
					}
				}
				
				log.trace("Track message");
				// track message
				MessageUtils.trackMessage(txToMonitor, isOutgoing, txService);
				
				
				log.trace("Post processing for rejected recips.");
				onPostprocessMessage(smtpMessage, result, isOutgoing, txToMonitor);
				
				log.trace("Sending to sta post process");
				staPostProcessSource.staPostProcess(smtpMessage);
				
				log.trace("Exiting Message<?> streamMsg");
			}
			catch (MessagingException e)
			{
				throw new RuntimeException(e);
			}
			finally
			{
				GatewayState.getInstance().unlockFromProcessing();
			}
		};
	}
	
	protected void onMessageRejected(SMTPMailMessage mail, NHINDAddressCollection recipients, NHINDAddress sender, boolean isOutgoing,
			Tx tx, Throwable t)
	{
		// if this is an outgoing IMF message, then we need to send a DSN message
		if (isOutgoing && tx != null && tx.getMsgType() == TxMessageType.IMF)
			sendDSN(tx, recipients, true);
	}
	
	
	protected void onPostprocessMessage(SMTPMailMessage mail, MessageProcessResult result, boolean isOutgoing, Tx tx)
	{
		// if there are rejected recipients and an outgoing IMF message, then we need to send a DSN message
		if (isOutgoing && tx != null && tx.getMsgType() == TxMessageType.IMF && result.getProcessedMessage().hasRejectedRecipients())
			sendDSN(tx, result.getProcessedMessage().getRejectedRecipients(), true);

	}	
	
	protected void sendDSN(Tx tx, NHINDAddressCollection undeliveredRecipeints, boolean useSenderAsPostmaster)
	{
		try
		{
			final Collection<MimeMessage> msgs = dsnCreator.createDSNFailure(tx, undeliveredRecipeints, useSenderAsPostmaster);
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
