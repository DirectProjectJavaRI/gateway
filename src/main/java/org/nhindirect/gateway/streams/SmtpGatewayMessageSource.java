package org.nhindirect.gateway.streams;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

@EnableBinding(SmtpGatewayMessageOutput.class)
public class SmtpGatewayMessageSource
{	
	@SuppressWarnings("deprecation")
	private static final Log LOGGER = LogFactory.getFactory().getInstance(SmtpGatewayMessageSource.class);		
	
	@Autowired
	@Qualifier(SmtpGatewayMessageOutput.SMTP_GATEWAY_MESSAGE_OUTPUT)
	private MessageChannel smtpGatewayChannel;
	
	@Output(SmtpGatewayMessageOutput.SMTP_GATEWAY_MESSAGE_OUTPUT)
	public <T> void sendMimeMessage(MimeMessage msg)
	{
		try
		{
			this.smtpGatewayChannel.send(SMTPMailMessageConverter.toStreamMessage(mimeMsgToSMTPMailMessage(msg)));
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to send SMTP message to gateway processor stream channel.", e);
		}
	}

	public static SMTPMailMessage mimeMsgToSMTPMailMessage(MimeMessage msg) throws MessagingException
	{
		final InternetAddress sender = (msg.getFrom() != null && msg.getFrom().length > 0) ? (InternetAddress)msg.getFrom()[0] : null;
		final List<InternetAddress> recipients = new ArrayList<>(); 
		for (Address addr : msg.getAllRecipients())
			recipients.add((InternetAddress) addr);
		
		return new SMTPMailMessage(msg, recipients, sender);
	}
}
