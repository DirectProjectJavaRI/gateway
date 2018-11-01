package org.nhindirect.gateway.streams;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.gateway.streams.processor.SmtpGatewayMessageProcessor;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.SpringBaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("streams")
public class SmtpGatewayMessageProcessor_receiveMessageTest extends SpringBaseTest
{
	@Autowired
	protected SmtpGatewayMessageInput channels;
	
	@Autowired 
	protected SmtpGatewayMessageProcessor processor;
	
	@SuppressWarnings("deprecation")
	@Test
	public void testReceiveSMTPMessage_outgoingMessage_assertProcessed() throws Exception
	{
		
		STASource staSource = spy(mock(STASource.class));
		processor.setSTASource(staSource);
		
		TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
		
		final String strMessage = TestUtils.readMessageResource("PlainOutgoingMessage.txt");
		
		final MimeMessage msg = new MimeMessage((Session)null, IOUtils.toInputStream(strMessage));
		
		SMTPMailMessage mailMsg = SmtpGatewayMessageSource.mimeMsgToSMTPMailMessage(msg);
				
		channels.smtpGatewayMessageInput().send(SMTPMailMessageConverter.toStreamMessage(mailMsg));
		
		verify(staSource, times(1)).staProcess((SMTPMailMessage)any());
	}
	
}
