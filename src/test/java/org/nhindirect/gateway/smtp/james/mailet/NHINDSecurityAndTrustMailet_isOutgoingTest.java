package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.mailet.MailetConfig;
import org.junit.Test;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.SpringBaseTest;


public class NHINDSecurityAndTrustMailet_isOutgoingTest extends SpringBaseTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		protected NHINDSecurityAndTrustMailet theMailet;
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
			Map<String,String> params = new HashMap<String, String>();
			
			return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
		}
		
		protected void setupMocks() 
		{
			theMailet = new NHINDSecurityAndTrustMailet();

			try
			{
				MailetConfig config = getMailetConfig();
				
				theMailet.init(config);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@SuppressWarnings("deprecation")
		@Override
		protected void performInner() throws Exception
		{

			MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource(getMessageToSend())));
			NHINDAddress sender = new NHINDAddress((InternetAddress)msg.getFrom()[0]);
			
			doAssertions(theMailet.isOutgoing(msg, sender));
		}
		
		protected String getMessageToSend()
		{
			return "PlainOutgoingMessage.txt";
		}
		
		
		protected void doAssertions(boolean b) throws Exception
		{
		}			
	}
	
	@Test
	public void testIsOutgoingTest_senderInDomain_assertTrue() throws Exception 
	{
		new TestPlan() 
		{
			
			@Override
			protected void doAssertions(boolean b) throws Exception
			{
				assertTrue(b);
			}			
		}.perform();
	}
	
	@Test
	public void testIsOutgoingTest_senderNotInDomain_assertFalse() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected String getMessageToSend()
			{
				return "MultipleRecipientsIncomingMessage.txt";
			}
			@Override
			protected void doAssertions(boolean b) throws Exception
			{
				assertFalse(b);
			}			
		}.perform();
	}
	
	@Test
	public void testIsOutgoingTest_encryptedMessageFromInternalDomain_assertFalse() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected String getMessageToSend()
			{
				return "EncryptedMessage.txt";
			}
			@Override
			protected void doAssertions(boolean b) throws Exception
			{
				assertFalse(b);
			}			
		}.perform();
	}
}
