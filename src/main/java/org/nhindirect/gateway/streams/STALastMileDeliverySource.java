package org.nhindirect.gateway.streams;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class STALastMileDeliverySource
{
	// Maps to the Spring Cloud Stream functional output binding name.
	protected static final String OUT_BINDING_NAME = "direct-sta-last-mile-out-0";
	
	@Autowired
	private StreamBridge streamBridge;	
	
	public <T> void staLastMile(SMTPMailMessage msg) 
	{
		streamBridge.send(OUT_BINDING_NAME, SMTPMailMessageConverter.toStreamMessage(msg));
	}
}
