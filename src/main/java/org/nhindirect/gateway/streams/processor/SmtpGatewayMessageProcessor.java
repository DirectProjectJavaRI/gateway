package org.nhindirect.gateway.streams.processor;

import javax.mail.MessagingException;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.gateway.smtp.GatewayState;
import org.nhindirect.gateway.smtp.SmtpAgent;
import org.nhindirect.gateway.streams.STASource;
import org.nhindirect.gateway.streams.SmtpGatewayMessageInput;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;


@EnableBinding(SmtpGatewayMessageInput.class)
public class SmtpGatewayMessageProcessor
{		
	private static final Logger LOGGER = LoggerFactory.getLogger(SmtpGatewayMessageProcessor.class);	
	
	@Autowired
	protected SmtpAgent smtpAgent;
	
	@Autowired
	protected STASource staSource;
	
	public SmtpGatewayMessageProcessor()
	{
	}

	public void setSmtpAgent(SmtpAgent smtpAgent)
	{
		this.smtpAgent = smtpAgent;
	}
	
	public void setSTASource(STASource staSource)
	{
		this.staSource = staSource;
	}	
	
	@StreamListener(target = SmtpGatewayMessageInput.SMTP_GATEWAY_MESSAGE_INPUT)
	public void processSmtpMessage(Message<?> streamMsg) throws MessagingException
	{
		final SMTPMailMessage smtpMessage = SMTPMailMessageConverter.fromStreamMessage(streamMsg);
		
		preProcessMessage(smtpMessage);
	}

	
	public void preProcessMessage(SMTPMailMessage smtpMessage) throws MessagingException
	{
			
		final NHINDAddress sender = MessageUtils.getMailSender(smtpMessage);
		
		LOGGER.info("SmtpGatewayMessageProcessor receiving message from sender " + sender.toString());
			

		boolean isOutgoing = false;
		GatewayState.getInstance().lockForProcessing();
		try
		{
			isOutgoing = MessageUtils.isOutgoing(smtpMessage.getMimeMessage(), sender, smtpAgent.getAgent());
		}
		finally
		{
			GatewayState.getInstance().unlockFromProcessing();
		}
		
		if (isOutgoing)
		{
			// TODO: Implement virus scanning and XD processing logic
		}
		
		// Send on to the STA
		staSource.staProcess(smtpMessage);
	}
}
