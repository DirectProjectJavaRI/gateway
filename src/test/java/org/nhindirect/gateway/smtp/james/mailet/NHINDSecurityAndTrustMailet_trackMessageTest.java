package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.mailet.MailetConfig;
import org.junit.Test;
import org.nhindirect.common.mail.MailStandard;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.AddressSource;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.parser.EntitySerializer;

public class NHINDSecurityAndTrustMailet_trackMessageTest extends SpringBaseTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		NHINDSecurityAndTrustMailet theMailet;
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			TestUtils.createGatewayConfig(getConfigName(), settingService, domainService);
			final Map<String,String> params = new HashMap<String, String>();
			
			return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
		}
		
		@Override
		protected void setupMocks() 
		{
			theMailet = new NHINDSecurityAndTrustMailet()
			{
				@Override
				protected TxService createTxServices()
				{
					return new MockTxService();
				}			
			};

			try
			{
				final MailetConfig config = getMailetConfig();
				
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
			final String originalMessage = TestUtils.readMessageResource(getMessageToSend());
			
			final MimeMessage msg = EntitySerializer.Default.deserialize(originalMessage);
			final MockMail theMessage = new MockMail(msg);
			final InternetAddress senderAddr = NHINDSecurityAndTrustMailet.getSender(theMessage);
			final NHINDAddress sender = new NHINDAddress(senderAddr, AddressSource.From);	
			
			final Tx tx = theMailet.getTxToTrack(msg, sender, new NHINDAddressCollection());
			
			theMailet.trackMessage(tx, isOutgoing());
			
			doAssertions((MockTxService)theMailet.txService);
		}
		
		protected boolean isOutgoing()
		{
			return true;
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
	public void testMonitorMessage_trackOutgoingIMFMessage_assertMessageTracked() throws Exception 
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
	public void testMonitorMessage_trackIncomingMFMessage_assertNotMessageTracked() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected boolean isOutgoing()
			{
				return false;
			}
			
			@Override
			protected void doAssertions(MockTxService service) throws Exception
			{
				assertEquals(0, service.txs.size());
			}			
		}.perform();
	}
	
	@Test
	public void testMonitorMessage_trackOutgoingMDNMessage_assertNotMessageTracked() throws Exception 
	{
		new TestPlan() 
		{
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
	public void testMonitorMessage_trackIncomingMDNMessage_assertNotMessageTracked() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected boolean isOutgoing()
			{
				return false;
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
	public void testMonitorMessage_trackOutgoingDSNMessage_assertNotMessageTracked() throws Exception 
	{
		new TestPlan() 
		{
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
	
	@Test
	public void testMonitorMessage_trackIncomingDSNMessage_assertNotMessageTracked() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected boolean isOutgoing()
			{
				return false;
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
