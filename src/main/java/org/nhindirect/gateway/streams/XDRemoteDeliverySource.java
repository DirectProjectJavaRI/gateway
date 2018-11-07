package org.nhindirect.gateway.streams;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

@EnableBinding(XDRemoteDeliveryOutput.class)
public class XDRemoteDeliverySource
{
	@Autowired
	@Qualifier(XDRemoteDeliveryOutput.XD_DELIVERY_MESSAGE_OUTPUT)
	private MessageChannel xdRemoteDeliveryChannel;
	
	@Output(XDRemoteDeliveryOutput.XD_DELIVERY_MESSAGE_OUTPUT)
	public <T> void xdRemoteDelivery(SMTPMailMessage msg) 
	{
		xdRemoteDeliveryChannel.send(SMTPMailMessageConverter.toStreamMessage(msg));
	}
}
