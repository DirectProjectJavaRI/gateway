package org.nhindirect.gateway.streams;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface SmtpGatewayMessageInput
{
	public static final String SMTP_GATEWAY_MESSAGE_INPUT = "direct-smtp-gateway-message-input";
	
	@Input(SMTP_GATEWAY_MESSAGE_INPUT)
	SubscribableChannel smtpGatewayMessageInput();
}
