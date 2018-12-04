package org.nhindirect.gateway.smtp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;

import org.junit.Test;
import org.nhindirect.common.audit.AuditContext;
import org.nhindirect.common.audit.AuditEvent;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.DefaultMessageEnvelope;
import org.nhindirect.stagent.MockAuditor;
import org.nhindirect.stagent.MockNHINDAgent;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.mail.notifications.MDNStandard;



public class DefaultSmtpAgent_AuditMessage_Test extends SpringBaseTest 
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
				TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
				
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
				
		protected abstract String getMessageToProcess() throws Exception;	
		
	}
	
	@Test
	public void testAuditIncomingMessage_AssertEventsAudited() throws Exception 
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
				DefaultSmtpAgent smtpAgent = (DefaultSmtpAgent)agent;
				assertNotNull(smtpAgent.getAuditor());
				assertTrue(smtpAgent.getAuditor() instanceof MockAuditor);
				
				assertTrue(auditor.getEvents().size() > 0);
				
				boolean foundIncomingType = false;
				boolean foundMDNType = false;
				for (Entry<AuditEvent, Collection<? extends AuditContext>> entry : auditor.getEvents().entrySet())
				{
					AuditEvent event = entry.getKey();
					assertEquals(event.getType(), "SMTP Direct Message Processing");
					
					if (event.getName().equals(AuditEvents.INCOMING_MESSAGE_NAME))
						foundIncomingType = true;
					else if (event.getName().equals(AuditEvents.PRODUCE_MDN_NAME))
					{
						boolean foundFinalRecip = false;
						boolean foundOrigMsgId = false;		
						boolean foundDisp = false;
						
						for (AuditContext ctx : entry.getValue())
						{
							if (ctx.getContextName().equals(MDNStandard.Headers.FinalRecipient))
							{
								foundFinalRecip = true;
							}
							else if (ctx.getContextName().equals(MDNStandard.Headers.OriginalMessageID))
							{
								foundOrigMsgId = true;
							}
							else if (ctx.getContextName().equals(MDNStandard.Headers.Disposition))
							{
								assertEquals("automatic-action/MDN-sent-automatically;processed", ctx.getContextValue());
								foundDisp = true;
							}							
							
						}
						
						assertTrue(foundFinalRecip);
						assertTrue(foundOrigMsgId);
						assertTrue(foundDisp);
						
						foundMDNType = true;
						// assert attributes of the event
						
					}
				}
				
				assertTrue(foundIncomingType);
				assertTrue(foundMDNType);
			}
			
		}.perform();
	}	
	
	@Test
	public void testAuditOutgoingMessage_AssertEventsAudited() throws Exception 
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
				DefaultSmtpAgent smtpAgent = (DefaultSmtpAgent)agent;
				assertNotNull(smtpAgent.getAuditor());
				assertTrue(smtpAgent.getAuditor() instanceof MockAuditor);
				
				assertTrue(auditor.getEvents().size() > 0);
				
				boolean foundOutgoingType = false;
				for (Entry<AuditEvent, Collection<? extends AuditContext>> entry : auditor.getEvents().entrySet())
				{
					AuditEvent event = entry.getKey();
					assertEquals(event.getType(), "SMTP Direct Message Processing");
					
					if (event.getName().equals("Outgoing Direct Message"))
						foundOutgoingType = true;
				}
				
				assertTrue(foundOutgoingType);
			}
			
		}.perform();
	}		
}
