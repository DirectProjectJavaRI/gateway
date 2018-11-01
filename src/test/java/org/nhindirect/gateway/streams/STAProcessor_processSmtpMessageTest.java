package org.nhindirect.gateway.streams;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.common.mail.streams.SMTPMailMessageConverter;
import org.nhindirect.gateway.smtp.SmtpAgent;
import org.nhindirect.gateway.streams.processor.STAProcessor;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.MutableAgent;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.cryptography.SMIMEStandard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("streams")
public class STAProcessor_processSmtpMessageTest extends SpringBaseTest
{
	@Autowired 
	protected STAInput channels;
	
	@Autowired 
	protected STAProcessor processor;
	
	@Autowired 
	protected SmtpAgent smtpAgent;
	
	@SuppressWarnings("deprecation")
	@Test
	public void testprocessSmtpMessage_outgoingMessage_assertProcessed() throws Exception
	{
		MutableAgent agent = (MutableAgent)smtpAgent.getAgent();
		agent.setPublicCertResolvers(Arrays.asList(agent.getPrivateCertResolver()));
		
		CaptureSTAPostProcessSource staPostProcessSource = new CaptureSTAPostProcessSource();
		
		processor.setSTAPostProcessSource(staPostProcessSource);
		
		TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
		
		final String strMessage = TestUtils.readMessageResource("PlainOutgoingMessage.txt");
		
		final MimeMessage msg = new MimeMessage((Session)null, IOUtils.toInputStream(strMessage));
		
		SMTPMailMessage mailMsg = SmtpGatewayMessageSource.mimeMsgToSMTPMailMessage(msg);
				
		channels.staInput().send(SMTPMailMessageConverter.toStreamMessage(mailMsg));
		
		assertNotNull(staPostProcessSource.processedMessage);
		assertTrue(SMIMEStandard.isEncrypted(staPostProcessSource.processedMessage));
	}
	
	protected static class CaptureSTAPostProcessSource extends STAPostProcessSource
	{
		MimeMessage processedMessage = null;
		@Override
		public <T> void staPostProcess(SMTPMailMessage msg) 
		{
			processedMessage = msg.getMimeMessage();
		}
	}
}
