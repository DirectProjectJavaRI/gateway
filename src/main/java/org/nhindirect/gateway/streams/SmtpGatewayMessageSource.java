package org.nhindirect.gateway.streams;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SmtpGatewayMessageSource
{	
	// Maps to the Spring Cloud Stream functional output binding name.
	protected static final String OUT_BINDING_NAME = "direct-smtp-gateway-message-out-0";
	
	@Autowired
	private StreamBridge streamBridge;
	
	public <T> void sendMimeMessage(MimeMessage msg)
	{
		try
		{
			streamBridge.send(OUT_BINDING_NAME, SMTPMailMessageConverter.toStreamMessage(mimeMsgToSMTPMailMessage(msg)));
		}
		catch (Exception e)
		{
			log.warn("Failed to send SMTP message to gateway processor stream channel.", e);
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
