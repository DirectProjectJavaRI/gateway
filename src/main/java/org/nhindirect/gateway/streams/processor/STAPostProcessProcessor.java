package org.nhindirect.gateway.streams.processor;


import javax.mail.MessagingException;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.common.rest.exceptions.ServiceException;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetail;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.streams.STAPostProcessInput;
import org.nhindirect.gateway.streams.SmtpRemoteDeliverySource;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.cryptography.SMIMEStandard;
import org.nhindirect.stagent.mail.notifications.MDNStandard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;

@EnableBinding(STAPostProcessInput.class)
public class STAPostProcessProcessor
{
	@Value("${direct.gateway.postprocess.ConsumeMDNProcessed:true}")
	protected boolean consumeMDNProcessed;
	
	@Autowired
	protected SmtpRemoteDeliverySource remoteDeliverySource;
	
	@Autowired
	protected TxService txService;
	
	@Autowired
	protected TxDetailParser txParser;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(STAPostProcessProcessor.class);	
			
	@StreamListener(target = STAPostProcessInput.STA_POST_PROCESS_INPUT)
	public void postProcessSmtpMessage(Message<?> streamMsg) throws MessagingException
	{
		final SMTPMailMessage smtpMessage = SMTPMailMessageConverter.fromStreamMessage(streamMsg);
		
		LOGGER.debug("STAPostProcessProcessor processing message from " + smtpMessage.getMailFrom().toString());
	
		/*
		 * If encrypted then at this point, then it's an outgoing message.
		 */
		final boolean  isOutgoing = SMIMEStandard.isEncrypted(smtpMessage.getMimeMessage());
		
		
		if (isOutgoing)
		{
			// TODO: Implement virus scanning and sending to delivery processor
			
			/*
			 * send remotely
			 */
			
			LOGGER.debug("Outgoing message.  Sending to remote delivery");
			
			remoteDeliverySource.remoteDelivery(smtpMessage, false);
		}
		else
		{
			/*
			 * need to check if this is an incoming notification message
			 * and check to see if it needs to suppressed from delivering to the 
			 * final destination
			 */
			if (!suppressAndTrackNotifications(smtpMessage))
			{
				// local delivery
				LOGGER.debug("Incoming message.  Sending to local delivery");
			}
		}
		
	}
	
	protected boolean suppressAndTrackNotifications(SMTPMailMessage smtpMessage) throws MessagingException
	{
		final NHINDAddressCollection recipients = MessageUtils.getMailRecipients(smtpMessage);
		
		final NHINDAddress sender = MessageUtils.getMailSender(smtpMessage);
			
		final Tx txToTrack = MessageUtils.getTxToTrack(smtpMessage.getMimeMessage(), sender, recipients, txParser);		
		
		boolean suppress = false;
		
		try
		{
			// first check if this a MDN processed message and if the consume processed flag is turned on
			final TxDetail detail = txToTrack.getDetail(TxDetailType.DISPOSITION);
			if (consumeMDNProcessed && txToTrack.getMsgType() == TxMessageType.MDN 
					&& detail != null && detail.getDetailValue().contains(MDNStandard.Disposition_Processed))
				suppress = true;
			// if the first rule does not apply, then go to the tx Service to see if the message should be suppressed
			else if (txService != null && txToTrack != null && txService.suppressNotification(txToTrack))
				suppress = true;
		}
		catch (ServiceException e)
		{
			// failing to call the txService should not result in an exception being thrown
			// from this service.
			LOGGER.warn("Failed to get notification suppression status from service.  Message will assume to not need supressing.");
		}
		
		// track message
		if (txToTrack != null && (txToTrack.getMsgType() == TxMessageType.DSN || 
				txToTrack.getMsgType() == TxMessageType.MDN))		
		{
			try
			{
				txService.trackMessage(txToTrack);
			}
			///CLOVER:OFF
			catch (ServiceException ex)
			{
				LOGGER.warn("Failed to submit message to monitoring service.", ex);
			}
			///CLOVER:ON
		}
		return suppress;
	}
}
