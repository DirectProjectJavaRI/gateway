package org.nhindirect.gateway.streams;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface STAPostProcessOutput
{
	public static final String STA_POST_PROCESS_OUTPUT = "direct-sta-post-process-output";
	
	@Output(STA_POST_PROCESS_OUTPUT)
	MessageChannel staPostProcessOutput();
}
