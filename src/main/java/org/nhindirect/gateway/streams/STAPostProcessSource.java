package org.nhindirect.gateway.streams;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

@EnableBinding(STAPostProcessOutput.class)
public class STAPostProcessSource
{
	@Autowired
	@Qualifier(STAPostProcessOutput.STA_POST_PROCESS_OUTPUT)
	private MessageChannel postProcessChannel;
	
	@Output(STAPostProcessOutput.STA_POST_PROCESS_OUTPUT)
	public <T> void staPostProcess(SMTPMailMessage msg) 
	{
		this.postProcessChannel.send(SMTPMailMessageConverter.toStreamMessage(msg));
	}
}
