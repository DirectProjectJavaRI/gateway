package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.Test;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.MimeMessage;


import org.apache.commons.io.IOUtils;
import org.apache.mailet.MailetConfig;
import org.nhindirect.common.mail.MailStandard;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.parser.EntitySerializer;


public class NHINDSecurityAndTrustMailet_monitorMessageTest extends SpringBaseTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		NHINDSecurityAndTrustMailet theMailet;
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			TestUtils.createGatewayConfig(getConfigName(), settingService, domainService);
			Map<String,String> params = new HashMap<String, String>();
			
			return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
		}
		
		@Override
		protected void setupMocks() 
		{
			NHINDSecurityAndTrustMailet mailet = new NHINDSecurityAndTrustMailet()
			{
				@Override
				protected TxService createTxServices()
				{
					return new MockTxService();
				}			
			};

			try
			{
				MailetConfig config = getMailetConfig();
				
				theMailet = spy(mailet);
				
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
			
			theMailet.service(theMessage);
			
			doAssertions((MockTxService)theMailet.txService);
		}
		
		protected String getMessageToSend()
		{
			return "PlainOutgoingMessage.txt";
		}
		
		protected String getConfigName()
		{
			return TestUtils.VALID_GATEWAY_CONFIG;
		}
		
		protected void doAssertions(MockTxService service) throws Exception
		{
		}			
	}
	
	@Test
	public void testMonitorMessage_trackTrustedOutgoingMessage_assertMessageTracked() throws Exception 
	{
		new TestPlan() 
		{
			@SuppressWarnings("deprecation")
			@Override
			protected void doAssertions(MockTxService service) throws Exception
			{
				
				assertEquals(1, service.txs.size());
				Tx tx = service.txs.iterator().next();
				assertEquals(TxMessageType.IMF, tx.getMsgType());
				
				MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource(getMessageToSend())));
				assertEquals(MailStandard.getHeader(msg, MailStandard.Headers.From).toLowerCase(Locale.getDefault()),
						tx.getDetail(TxDetailType.FROM).getDetailValue());
			}			
		}.perform();
	}
	
	@Test
	public void testMonitorMessage_trackMDNMessage_assertMessageNotTracked() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected String getConfigName()
			{
				return TestUtils.VALID_GATEWAY_STATELINE_CONFIG;
			}
			
			@Override
			protected String getMessageToSend()
			{
				return "MDNMessage.txt";
			}
			
			@Override
			protected void doAssertions(MockTxService service) throws Exception
			{
				assertEquals(0, service.txs.size());
			}			
		}.perform();
	}
	
	@Test
	public void testMonitorMessage_trackDSNMessage_assertMessageNotTracked() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected String getConfigName()
			{
				return TestUtils.VALID_GATEWAY_STATELINE_CONFIG;
			}
			
			@Override
			protected String getMessageToSend()
			{
				return "DSNMessage.txt";
			}
			
			@Override
			protected void doAssertions(MockTxService service) throws Exception
			{
				assertEquals(0, service.txs.size());
			}			
		}.perform();
	}
}
