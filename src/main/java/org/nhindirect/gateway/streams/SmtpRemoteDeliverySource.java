package org.nhindirect.gateway.streams;

import java.util.HashMap;
import java.util.Map;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class SmtpRemoteDeliverySource
{
	// Maps to the Spring Cloud Stream functional output binding name.
	protected static final String OUT_BINDING_NAME = "direct-smtp-remote-delivery-out-0";
	
	@Autowired
	private StreamBridge streamBridge;
	
	public static final String REMOTE_DELIVERY_GROUPED = "REMOTE_DELIVERY_GROUPED";	
	
	public <T> void remoteDelivery(SMTPMailMessage msg, boolean grouped) 
	{
		final Map<String, Object> headerMap = new HashMap<>();
		headerMap.put(REMOTE_DELIVERY_GROUPED, true);
		
		final Message<?> streamMsg = (!grouped) ? SMTPMailMessageConverter.toStreamMessage(msg) :
			SMTPMailMessageConverter.toStreamMessage(msg, headerMap);
		
		streamBridge.send(OUT_BINDING_NAME, streamMsg);
	}
}
