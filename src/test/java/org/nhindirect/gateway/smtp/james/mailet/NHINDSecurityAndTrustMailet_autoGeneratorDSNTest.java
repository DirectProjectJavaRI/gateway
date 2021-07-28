package org.nhindirect.gateway.smtp.james.mailet;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.apache.mailet.MailetConfig;

import org.nhindirect.common.mail.MailStandard;
import org.nhindirect.common.mail.MailUtil;
import org.nhindirect.common.mail.dsn.DSNStandard;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.impl.NoOpTxServiceClient;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.parser.EntitySerializer;

import com.sun.mail.dsn.DeliveryStatus;

public class NHINDSecurityAndTrustMailet_autoGeneratorDSNTest extends SpringBaseTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		NHINDSecurityAndTrustMailet theMailet;
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
			
			Map<String,String> params = new HashMap<String, String>();
			
			return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
		}
		
		@Override
		protected void setupMocks() 
		{
			try
			{

			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
	        	
			theMailet = new NHINDSecurityAndTrustMailet() 
			{
				@Override
				protected TxService createTxServices()
				{
					return new NoOpTxServiceClient();
				}
			};

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
		
		@Override
		protected void performInner() throws Exception
		{			
			// encrypt
			String originalMessage = TestUtils.readMessageResource(getMessageToSend());
			
			MimeMessage msg = EntitySerializer.Default.deserialize(originalMessage);
			
			MockMail theMessage = new MockMail(msg);
			
			try
			{
				theMailet.service(theMessage);
			}
			catch (Exception e)
			{
				/* no-op */
			}
			doAssertions((MockMailetContext)theMailet.getMailetContext());
		}
		
		protected String getMessageToSend()
		{
			return "PlainOutgoingMessage.txt";
		}
		
		protected void doAssertions(MockMailetContext context) throws Exception
		{
		}			
	}
	
	@Test
	public void testAutoGeneratorDSN_generateDNSForGeneralUntrustedRecips_assertDSNSent() throws Exception 
	{
		new TestPlan() 
		{
			
			@Override
			protected String getMessageToSend()
			{
				return "PlainUntrustedOutgoingMessage.txt";
			}
			
			@Override
			protected void doAssertions(MockMailetContext context) throws Exception
			{
				assertEquals(1, context.getSentMessages().size());
				MimeMessage dsnMessage = context.getSentMessages().iterator().next().getMessage();
				
				String originalMessageString = TestUtils.readMessageResource(getMessageToSend());
				
				MimeMessage originalMsg = EntitySerializer.Default.deserialize(originalMessageString);
				
				assertEquals(MailStandard.getHeader(originalMsg, MailStandard.Headers.From).toLowerCase(Locale.getDefault()),
						MailStandard.getHeader(dsnMessage, MailStandard.Headers.To).toLowerCase(Locale.getDefault()));
				
				NHINDAddress originalSenderAddress = new NHINDAddress(MailStandard.getHeader(originalMsg, MailStandard.Headers.From));
				NHINDAddress dsnFromAddress = new NHINDAddress(MailStandard.getHeader(dsnMessage, MailStandard.Headers.From));
				
				assertTrue(dsnFromAddress.getHost().toLowerCase(Locale.getDefault()).contains(originalSenderAddress.getHost().toLowerCase(Locale.getDefault())));

				
			}			
		}.perform();
	}
	
	@Test
	public void testAutoGeneratorDSN_generateDNSForGeneralMultiRecipUntrustedRecips_assertDSNSent() throws Exception 
	{
		new TestPlan() 
		{
			
			@Override
			protected String getMessageToSend()
			{
				return "PlainOutgoingMessageWithRejectedRecips.txt";
			}
			
			@Override
			protected void doAssertions(MockMailetContext context) throws Exception
			{
				assertEquals(1, context.getSentMessages().size());
				MimeMessage dsnMessage = context.getSentMessages().iterator().next().getMessage();
				
				String originalMessageString = TestUtils.readMessageResource(getMessageToSend());
				
				MimeMessage originalMsg = EntitySerializer.Default.deserialize(originalMessageString);
				
				assertEquals(MailStandard.getHeader(originalMsg, MailStandard.Headers.From).toLowerCase(Locale.getDefault()),
						MailStandard.getHeader(dsnMessage, MailStandard.Headers.To).toLowerCase(Locale.getDefault()));
				
				final DeliveryStatus status = new DeliveryStatus(new ByteArrayInputStream(MailUtil.serializeToBytes(dsnMessage)));
				
				final String rejectRecip = DSNStandard.getFinalRecipients(status);
				assertEquals("someotherrecip@nontrustedomain.org", rejectRecip);
				
			}			
		}.perform();
	}
	
	@Test
	public void testAutoGeneratorDSN_generateDNSForReliableUntrustedRecips_assertDSNSent() throws Exception 
	{
		new TestPlan() 
		{
			
			@Override
			protected String getMessageToSend()
			{
				return "PlainUntrustedReliableOutgoingMessage.txt";
			}
			
			@Override
			protected void doAssertions(MockMailetContext context) throws Exception
			{
				assertEquals(1, context.getSentMessages().size());
				
				MimeMessage dsnMessage = context.getSentMessages().iterator().next().getMessage();
				
				String originalMessageString = TestUtils.readMessageResource(getMessageToSend());
				
				MimeMessage originalMsg = EntitySerializer.Default.deserialize(originalMessageString);
				
				assertEquals(MailStandard.getHeader(originalMsg, MailStandard.Headers.From).toLowerCase(Locale.getDefault()),
						MailStandard.getHeader(dsnMessage, MailStandard.Headers.To).toLowerCase(Locale.getDefault()));
				
				NHINDAddress originalSenderAddress = new NHINDAddress(MailStandard.getHeader(originalMsg, MailStandard.Headers.From));
				NHINDAddress dsnFromAddress = new NHINDAddress(MailStandard.getHeader(dsnMessage, MailStandard.Headers.From));
				
				assertTrue(dsnFromAddress.getHost().toLowerCase(Locale.getDefault()).contains(originalSenderAddress.getHost().toLowerCase(Locale.getDefault())));
			}			
		}.perform();
	}
	
	
	@Test
	public void testAutoGeneratorDSN_untrustedMDNMessage_assertNoDSNSent() throws Exception 
	{
		new TestPlan() 
		{
			
			@Override
			protected String getMessageToSend()
			{
				return "MDNMessage.txt";
			}
			
			@Override
			protected void doAssertions(MockMailetContext context) throws Exception
			{
				assertEquals(0, context.getSentMessages().size());
			}			
		}.perform();
	}
}
