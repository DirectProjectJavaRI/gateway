package org.nhindirect.gateway.streams;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface XDRemoteDeliveryOutput
{
	public static final String XD_DELIVERY_MESSAGE_OUTPUT = "direct-xd-delivery-output";
	
	@Output(XD_DELIVERY_MESSAGE_OUTPUT)
	MessageChannel remoteXDDeliveryOutput();
}
