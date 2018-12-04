package org.nhindirect.gateway.streams;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface SmtpRemoteDeliveryOutput
{
	public static final String SMTP_REMOTE_DELIVERY_MESSAGE_OUTPUT = "direct-smtp-remote-delivery-output";
	
	@Output(SMTP_REMOTE_DELIVERY_MESSAGE_OUTPUT)
	MessageChannel remoteDeliveryOutput();
}
