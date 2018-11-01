package org.nhindirect.gateway.streams;


import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

@EnableBinding(STAOutput.class)
public class STASource
{
	@Autowired
	@Qualifier(STAOutput.STA_OUTPUT)
	private MessageChannel staChannel;
	
	@Output(STAOutput.STA_OUTPUT)
	public <T> void staProcess(SMTPMailMessage msg) 
	{
		this.staChannel.send(SMTPMailMessageConverter.toStreamMessage(msg));
	}
}
