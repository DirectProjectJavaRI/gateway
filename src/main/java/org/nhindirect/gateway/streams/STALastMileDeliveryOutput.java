package org.nhindirect.gateway.streams;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface STALastMileDeliveryOutput
{
	public static final String STA_LAST_MILE_OUTPUT = "direct-sta-last-mile-output";
	
	@Output(STA_LAST_MILE_OUTPUT)
	MessageChannel staLastMileOutput();
}
