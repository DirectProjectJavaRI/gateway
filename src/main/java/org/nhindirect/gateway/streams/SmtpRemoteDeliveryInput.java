package org.nhindirect.gateway.streams;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface SmtpRemoteDeliveryInput
{
	public static final String SMTP_REMOTE_DELIVERY_MESSAGE_INPUT = "direct-smtp-remote-delivery-input";
	
	@Input(SMTP_REMOTE_DELIVERY_MESSAGE_INPUT)
	SubscribableChannel smtpRemoteDeliveryInput();
}
