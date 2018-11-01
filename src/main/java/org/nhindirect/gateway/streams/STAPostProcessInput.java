package org.nhindirect.gateway.streams;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface STAPostProcessInput
{
    public static final String STA_POST_PROCESS_INPUT = "direct-sta-post-process-input";

    @Input(STA_POST_PROCESS_INPUT)
	SubscribableChannel staPostProcessInput();
}
