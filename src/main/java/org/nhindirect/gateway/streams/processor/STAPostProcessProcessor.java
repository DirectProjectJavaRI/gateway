package org.nhindirect.gateway.streams.processor;


import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.nhind.config.rest.DomainService;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.common.rest.exceptions.ServiceException;
import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetail;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.streams.STALastMileDeliverySource;
import org.nhindirect.gateway.streams.SmtpGatewayMessageSource;
import org.nhindirect.gateway.streams.SmtpRemoteDeliverySource;
import org.nhindirect.gateway.streams.XDRemoteDeliverySource;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.cryptography.SMIMEStandard;
import org.nhindirect.stagent.mail.notifications.MDNStandard;
import org.nhindirect.xd.routing.RoutingResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class STAPostProcessProcessor
{
	@Value("${direct.gateway.postprocess.ConsumeMDNProcessed:true}")
	protected boolean consumeMDNProcessed;
	
	@Value("${direct.gateway.xd.enabled:true}")
	protected boolean xdEnabled;	

	@Value("${direct.gateway.postprocess.routeLocalRecipientToGateway:true}")
	protected boolean routeLocalRecipientToGateway;	
	
	@Autowired
	protected SmtpRemoteDeliverySource remoteDeliverySource;
	
	@Autowired 
	protected XDRemoteDeliverySource xdRemoteDeliverySource;
	
	@Autowired 
	protected STALastMileDeliverySource lastMileSource;
	
	@Autowired
	protected RoutingResolver routingResolver;
	
	@Autowired
	protected TxService txService;
	
	@Autowired
	protected TxDetailParser txParser;
	
	@Autowired
	protected DomainService domainService;
	
	@Autowired
	protected SmtpGatewayMessageSource smtpMessageSource;	
			
	@Bean
	public Consumer<Message<?>> directStaPostProcessInput()
	{
		return streamMsg ->
		{
			try
			{
				final SMTPMailMessage smtpMessage = SMTPMailMessageConverter.fromStreamMessage(streamMsg);
				
				log.debug("STAPostProcessProcessor processing message from " + smtpMessage.getMailFrom().toString());
			
				/*
				 * If encrypted then at this point, then it's an outgoing message.
				 */
				final boolean  isOutgoing = SMIMEStandard.isEncrypted(smtpMessage.getMimeMessage());
				boolean  isXDrecipient = false;

				if (isOutgoing)
				{
					// TODO: Implement virus scanning
					
					/*
					 * send remotely
					 */
					
					log.info("Sending outgoing message to remote/loopback delivery");
					
					/*
					 * If the routeLocalRecipientToGateway flag is set, we will try to by-pass the remote delivery gateway
					 * for local recipients and send the message directly back to the STA via the gateway stream
					 */
					if (routeLocalRecipientToGateway && isLocalRecipients(smtpMessage))
						smtpMessageSource.sendMimeMessage(smtpMessage.getMimeMessage());
					
					remoteDeliverySource.remoteDelivery(smtpMessage, false);
				}
				else
				{
               /*
                * send locally
                */

               log.info("Sending incoming message to local/loopback delivery");

               /*
					 * need to check if this is an incoming notification message
					 * and check to see if it needs to suppressed from delivering to the 
					 * final destination
					 */
					if (suppressAndTrackNotifications(smtpMessage))
					{
						log.debug("Incoming notification message with id " + smtpMessage.getMimeMessage().getMessageID() + " will be suppressed.  Eating the message");
						return;
					}
		
					
					/*
					 * Determine if there are any XD recipients
					 */
					if (xdEnabled)
					{
                  log.debug("XD endpoints enabled");

                  final List<InternetAddress> xdRecipients = smtpMessage.getRecipientAddresses().stream()
							.filter(addr -> routingResolver.isXdEndpoint(addr.getAddress())).collect(Collectors.toList()); 
						
						if (!xdRecipients.isEmpty())
						{
							final SMTPMailMessage xdBoundMessage = new SMTPMailMessage(smtpMessage.getMimeMessage(), xdRecipients, 
									smtpMessage.getMailFrom());

                     log.info("XD recipient.  Sending to XD local delivery");
                     isXDrecipient = true;
                     xdRemoteDeliverySource.xdRemoteDelivery(xdBoundMessage);
						}
					}

					if(!isXDrecipient) {
                  /*
                   * Now do final delivery for non XD recipients.  This just simply puts the message a channel that will actually do
                   * the final delivery.  There could be any possibility of implementers listening on this channel, and we will
                   * leave it to those implementations to do their work.  Because this is using asycn delivery to the final destination,
                   * it is up the the final delivery implementation to generate a negative MDN/DSN if final delivery cannot be performed.
                   */
                  log.info("SMTP recipient.  Sending to SMTP local delivery");
                  lastMileSource.staLastMile(smtpMessage);
               }
				}
			}
			catch (MessagingException e)
			{
				throw new RuntimeException(e);
			}
		};
		
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
			log.warn("Failed to get notification suppression status from service.  Message will assume to not need supressing.");
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
				log.warn("Failed to submit message to monitoring service.", ex);
			}
			///CLOVER:ON
		}
		return suppress;
	}
	
	public boolean isLocalRecipients(SMTPMailMessage smtpMessage)
	{

		for (InternetAddress addr : smtpMessage.getRecipientAddresses())
		{
	    	final NHINDAddress nhinAddr = new NHINDAddress(addr);
	    	final String domain = nhinAddr.getHost();
	    	
	    	try
	    	{
		    	if (domainService.getDomain(domain) != null)
		    		return true;
	    	}
	    	catch (Exception e)
	    	{
	    		throw new IllegalStateException("Could not get local domain status for domain " + domain);
	    	}
		}

		return false;
	}
}
