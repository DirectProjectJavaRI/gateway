package org.nhindirect.gateway.streams;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

@EnableBinding(STALastMileDeliveryOutput.class)
public class STALastMileDeliverySource
{
	@Autowired
	@Qualifier(STALastMileDeliveryOutput.STA_LAST_MILE_OUTPUT)
	private MessageChannel lastMileChannel;
	
	@Output(STALastMileDeliveryOutput.STA_LAST_MILE_OUTPUT)
	public <T> void staLastMile(SMTPMailMessage msg) 
	{
		this.lastMileChannel.send(SMTPMailMessageConverter.toStreamMessage(msg));
	}
}
