package org.nhindirect.gateway.streams;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface STAInput
{
    public static final String STA_INPUT = "direct-sta-processor-input";

    @Input(STA_INPUT)
	SubscribableChannel staInput();
}
