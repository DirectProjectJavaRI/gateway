package org.nhindirect.gateway.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.mail.Message.RecipientType;

import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.stagent.mail.Message;
import org.nhindirect.stagent.mail.notifications.NotificationHelper;
import org.nhindirect.stagent.mail.notifications.NotificationMessage;
import org.nhindirect.stagent.DefaultMessageEnvelope;
import org.nhindirect.stagent.MockAuditor;
import org.nhindirect.stagent.MockNHINDAgent;
import org.nhindirect.stagent.SpringBaseTest;


public class DefaultSmtpAgent_ProcessMessage_Test extends SpringBaseTest 
{
	
	abstract class TestPlan extends BaseTestPlan 
	{
		protected MockAuditor auditor = new MockAuditor();
		protected DefaultSmtpAgent agent;
		
		@Override
		public void setupMocks()
		{
			try
			{
				
				TestUtils.createGatewayConfig(getConfigFileName(), settingService, domainService);
				
				agent = (DefaultSmtpAgent)SmtpAgentFactory.getInstance(certService, bundleService, domainService, anchorService, settingService, 
						certPolService, auditor, keyStoreMgr).createSmtpAgent();
				
				agent.setAgent(new MockNHINDAgent(Arrays.asList(new String[] {"cerner.com", "securehealthemail.com"})));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		protected void performInner() throws Exception 
		{
			DefaultMessageEnvelope env = new DefaultMessageEnvelope(getMessageToProcess());
			
			MessageProcessResult result = agent.processMessage(env.getMessage(), env.getRecipients(), env.getSender());			
			doAssertions(result);
		}	
	
		protected void doAssertions(MessageProcessResult result) throws Exception
		{
		}
		
		
		protected String getConfigFileName()
		{
			return TestUtils.VALID_GATEWAY_CONFIG;
		}
				
		protected abstract String getMessageToProcess() throws Exception;	
	}
	
	@Test
	public void testProcessValidIncomingMessage_AutoResponseTrue_NOMDNRequest_AssertSuccessfulResultWithAnMDNMessage() throws Exception 
	{
		new TestPlan() 
		{			
			protected String getMessageToProcess() throws Exception
			{
				return TestUtils.readMessageResource("PlainIncomingMessage.txt");
			}	
			
			@Override
			protected void doAssertions(MessageProcessResult result) throws Exception
			{
				assertNotNull(result);
				assertNotNull(result.getProcessedMessage());
				
				assertNotNull(result);
				assertNotNull(result.getProcessedMessage());
				assertNotNull(result.getProcessedMessage().getMessage());
				assertNotNull(result.getNotificationMessages());
				assertTrue(result.getNotificationMessages().size() > 0);
				
				// get the first message
				NotificationMessage notiMsg = result.getNotificationMessages().iterator().next();
				
				assertEquals(1, notiMsg.getRecipients(RecipientType.TO).length);
				
				Message processedMessage = result.getProcessedMessage().getMessage();
				String processedSender = processedMessage.getFrom()[0].toString();
				String notiRecip = notiMsg.getRecipients(RecipientType.TO)[0].toString();
				// make sure the to and from are the same
				assertEquals(processedSender, notiRecip);				
			}
			
		}.perform();
	}
	
	@Test
	public void testProcessValidIncomingMessageWithMDNRequest_AssertSuccessfulResultWithNoBounces() throws Exception 
	{
		new TestPlan() 
		{			
			protected String getMessageToProcess() throws Exception
			{
				return TestUtils.readMessageResource("PlainIncomingMessage.txt");
			}	
			
			@Override
			protected void performInner() throws Exception 
			{
				DefaultMessageEnvelope env = new DefaultMessageEnvelope(getMessageToProcess());
				
				// add the notification request
				NotificationHelper.requestNotification(env.getMessage());
				
				MessageProcessResult result = agent.processMessage(env.getMessage(), env.getRecipients(), env.getSender());			
				doAssertions(result);
			}
			
			@Override
			protected void doAssertions(MessageProcessResult result) throws Exception
			{
				assertNotNull(result);
				assertNotNull(result.getProcessedMessage());
				assertNotNull(result.getProcessedMessage().getMessage());
				assertNotNull(result.getNotificationMessages());
				assertTrue(result.getNotificationMessages().size() > 0);
				
				// get the first message
				NotificationMessage notiMsg = result.getNotificationMessages().iterator().next();
				
				assertEquals(1, notiMsg.getRecipients(RecipientType.TO).length);
				
				Message processedMessage = result.getProcessedMessage().getMessage();
				String processedSender = processedMessage.getFrom()[0].toString();
				String notiRecip = notiMsg.getRecipients(RecipientType.TO)[0].toString();
				// make sure the to and from are the same
				assertEquals(processedSender, notiRecip);
				
			}
			
		}.perform();
	}
	

	@Test
	public void testProcessValidOutgoingMessageWithMDNRequest_AssertSuccessfulResult() throws Exception 
	{
		new TestPlan() 
		{			
			protected String getMessageToProcess() throws Exception
			{
				return TestUtils.readMessageResource("PlainOutgoingMessage.txt");
			}	
			
			@Override
			protected void performInner() throws Exception 
			{
				DefaultMessageEnvelope env = new DefaultMessageEnvelope(getMessageToProcess());
				
				// add the notification request
				NotificationHelper.requestNotification(env.getMessage());
				
				MessageProcessResult result = agent.processMessage(env.getMessage(), env.getRecipients(), env.getSender());			
				doAssertions(result);
			}			
			
			@Override
			protected void doAssertions(MessageProcessResult result) throws Exception
			{
				assertNotNull(result);
				assertNotNull(result.getProcessedMessage());
			}
			
		}.perform();
	}	

	@Test
	public void testProcessValidOutgoingMessage_AssertSuccessfulResultWithNoBounces() throws Exception 
	{
		new TestPlan() 
		{			
			protected String getMessageToProcess() throws Exception
			{
				return TestUtils.readMessageResource("PlainOutgoingMessage.txt");
			}	
			
			@Override
			protected void doAssertions(MessageProcessResult result) throws Exception
			{
				assertNotNull(result);
				assertNotNull(result.getProcessedMessage());
			}
			
		}.perform();
	}	
	
	@Test
	public void testUniitializedNHINDAgent_AssertUninitializedException() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			public void setupMocks()
			{
				super.setupMocks();
				// a little bad magic to set the private agent to null
				try
				{
					Field field = agent.getClass().getDeclaredField("agent");
					field.setAccessible(true);
					field.set(agent, null);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
			
			protected String getMessageToProcess() throws Exception
			{
				return TestUtils.readMessageResource("PlainIncomingMessage.txt");
			}	
			
			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof SmtpAgentException);
				SmtpAgentException ex = (SmtpAgentException)exception;
				assertEquals(SmtpAgentError.Uninitialized, ex.getError());
			}

			
		}.perform();
	}	
	
	
}
