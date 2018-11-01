package org.nhindirect.gateway.streams;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface STAOutput
{
	public static final String STA_OUTPUT = "direct-sta-processor-output";
	
	@Output(STA_OUTPUT)
	MessageChannel staOutput();
}
