package org.nhindirect.gateway.streams.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.nhindirect.gateway.streams.STAInput;
import org.nhindirect.gateway.streams.STAPostProcessSource;
import org.nhindirect.gateway.streams.SmtpGatewayMessageSource;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.mail.notifications.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;

@EnableBinding(STAInput.class)
public class STAProcessor
{
	private static final Logger LOGGER = LoggerFactory.getLogger(STAProcessor.class);	
	
	@Autowired
	protected SmtpAgent smtpAgent;	
	
	@Autowired
	protected TxDetailParser txParser;
	
	@Autowired
	protected SmtpGatewayMessageSource smtpMessageSource;
	
	@Autowired
	protected TxService txService;
	
	@Autowired
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
	
	@StreamListener(target = STAInput.STA_INPUT)
	public void processSmtpMessage(Message<?> streamMsg) throws MessagingException
	{
		MessageProcessResult result = null;
		
		Tx txToMonitor = null;	
		
		SMTPMailMessage smtpMessage = SMTPMailMessageConverter.fromStreamMessage(streamMsg);
		
		final NHINDAddressCollection recipients = MessageUtils.getMailRecipients(smtpMessage);
		
		final NHINDAddress sender = MessageUtils.getMailSender(smtpMessage);
		
		boolean isOutgoing = false;
		GatewayState.getInstance().lockForProcessing();
		try
		{
			isOutgoing = MessageUtils.isOutgoing(smtpMessage.getMimeMessage(), sender, smtpAgent.getAgent());
			
			// recipients can get modified by the security and trust agent, so make a local copy
			// before processing
			final NHINDAddressCollection originalRecipList = NHINDAddressCollection.create(recipients);
			
			try
			{
				// process the message with the agent stack
				LOGGER.info("Calling stapProcessor.processSmtpMessage");
				result = smtpAgent.processMessage(smtpMessage.getMimeMessage(), recipients, sender);
				LOGGER.info("Finished calling agent.processMessage");
				
				if (result == null)
				{				
					LOGGER.info("Failed to process message.  processMessage returned null.");		
					
					onMessageRejected(smtpMessage, originalRecipList, sender, isOutgoing, txToMonitor, null);
					
					
					LOGGER.info("Exiting service(Mail mail)");
					return;
				}
			}	
			catch (Throwable e)
			{
				// catch all
				
				LOGGER.info("Failed to process message: " + e.getMessage(), e);					
				
				onMessageRejected(smtpMessage, originalRecipList, sender, isOutgoing, txToMonitor, e);
				
				return;
			}
			
			LOGGER.info("Updating SMTPMailMessage message with processed result");
			if (result.getProcessedMessage() != null)
			{
				smtpMessage = new SMTPMailMessage((MimeMessage)result.getProcessedMessage().getMessage(), 
						(List<InternetAddress>)recipients.toInternetAddressCollection(), 
						(InternetAddress)sender);
			}
			else
			{
				/*
				 * TODO: Handle exception... GHOST the message for now and eat it
				 */		
				LOGGER.info("Processed message is null.  Eat the message.");
	
				return;
			}
			
			LOGGER.info("Removing reject recipients from the RCTP headers");
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
			
			LOGGER.info("Handling sending MDN messages");
			/*
			 * Handle sending MDN messages
			 */
			final Collection<NotificationMessage> notifications = result.getNotificationMessages();
			if (notifications != null && notifications.size() > 0)
			{
				LOGGER.info("MDN messages requested.  Sending MDN \"processed\" messages");
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
						LOGGER.error("Error sending MDN message.", t);
					}
				}
			}
			
			LOGGER.info("Track message");
			// track message
			MessageUtils.trackMessage(txToMonitor, isOutgoing, txService);
			
			
			LOGGER.info("Post processing for rejected recips.");
			onPostprocessMessage(smtpMessage, result, isOutgoing, txToMonitor);
			
			LOGGER.info("Sending to sta post process");
			staPostProcessSource.staPostProcess(smtpMessage);
			
			LOGGER.trace("Exiting Message<?> streamMsg");
		}
		finally
		{
			GatewayState.getInstance().unlockFromProcessing();
		}
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
			LOGGER.error("Error sending DSN failure message.", e);
		}
	}	
}
