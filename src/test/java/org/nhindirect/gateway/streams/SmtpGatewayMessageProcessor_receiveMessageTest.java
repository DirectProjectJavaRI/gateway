package org.nhindirect.gateway.streams;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.nhindirect.common.mail.SMTPMailMessage;
import org.nhindirect.gateway.streams.processor.SmtpGatewayMessageProcessor;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.TestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

public class SmtpGatewayMessageProcessor_receiveMessageTest
{	
	@Autowired 
	protected SmtpGatewayMessageProcessor processor;
	
	@SuppressWarnings("deprecation")
	@Test
	public void testReceiveSMTPMessage_outgoingMessage_assertProcessed() throws Exception
	{
		final Properties props = new Properties();
		props.load(FileUtils.openInputStream(new File("./src/test/resources/bootstrap.properties")));
		props.setProperty("spring.main.web-application-type", "none");
		
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(
						TestApplication.class)).profiles("streams").properties(props)
				.run("")) 
		{
		
			final SettingService settingService = context.getBean(SettingService.class);
			final DomainService domainService = context.getBean(DomainService.class);
			final SmtpGatewayMessageProcessor processor = context.getBean(SmtpGatewayMessageProcessor.class);
			final SmtpGatewayMessageSource messageSource = context.getBean(SmtpGatewayMessageSource.class);
			
			STASource staSource = spy(mock(STASource.class));
			processor.setSTASource(staSource);
			
			TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
			
			final String strMessage = TestUtils.readMessageResource("PlainOutgoingMessage.txt");
			
			final MimeMessage msg = new MimeMessage((Session)null, IOUtils.toInputStream(strMessage));
			
			messageSource.sendMimeMessage(msg);
			
			verify(staSource, times(1)).staProcess((SMTPMailMessage)any());
		};
	}
	
}
