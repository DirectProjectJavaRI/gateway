package org.nhindirect.gateway.streams;

import java.util.HashMap;
import java.util.Map;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@EnableBinding(SmtpRemoteDeliveryOutput.class)
public class SmtpRemoteDeliverySource
{
	public static final String REMOTE_DELIVERY_GROUPED = "REMOTE_DELIVERY_GROUPED";	
	
	@Autowired
	@Qualifier(SmtpRemoteDeliveryOutput.SMTP_REMOTE_DELIVERY_MESSAGE_OUTPUT)
	private MessageChannel remoteDeliveryChannel;
	
	@Output(SmtpRemoteDeliveryOutput.SMTP_REMOTE_DELIVERY_MESSAGE_OUTPUT)
	public <T> void remoteDelivery(SMTPMailMessage msg, boolean grouped) 
	{
		final Map<String, Object> headerMap = new HashMap<>();
		headerMap.put(REMOTE_DELIVERY_GROUPED, true);
		
		final Message<?> streamMsg = (!grouped) ? SMTPMailMessageConverter.toStreamMessage(msg) :
			SMTPMailMessageConverter.toStreamMessage(msg, headerMap);
		
		this.remoteDeliveryChannel.send(streamMsg);

	}
}
