package org.nhindirect.gateway.streams.processor;

import java.util.function.Consumer;

import javax.mail.MessagingException;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.gateway.smtp.GatewayState;
import org.nhindirect.gateway.smtp.SmtpAgent;
import org.nhindirect.gateway.streams.STASource;
import org.nhindirect.gateway.util.MessageUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class SmtpGatewayMessageProcessor
{		
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
	
	@Bean
	public Consumer<Message<?>> directSmtpGatewayMessageInput() throws MessagingException
	{
		return streamMsg -> 
		{		
			try 
			{
				final SMTPMailMessage smtpMessage = SMTPMailMessageConverter.fromStreamMessage(streamMsg);
			
				preProcessMessage(smtpMessage);
			}
			catch (Exception e )
			{
				throw new RuntimeException("Failed to process message.", e);
			}
		};
	}

	
	public void preProcessMessage(SMTPMailMessage smtpMessage) throws MessagingException
	{
			
		final NHINDAddress sender = MessageUtils.getMailSender(smtpMessage);
		
		log.info("SmtpGatewayMessageProcessor receiving message from sender " + sender.toString());
			

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
