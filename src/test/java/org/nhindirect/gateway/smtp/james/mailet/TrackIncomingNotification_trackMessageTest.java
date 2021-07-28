package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.nhindirect.stagent.parser.EntitySerializer;


public class TrackIncomingNotification_trackMessageTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		TrackIncomingNotification theMailet;
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			final Map<String,String> params = new HashMap<String, String>();			
			return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
		}
		
		@Override
		protected void setupMocks() 
		{
			theMailet = new TrackIncomingNotification()
			{
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
			
			theMailet.service(theMessage);
			
			doAssertions((MockTxService)theMailet.txService);
		}

		protected String getMessageToSend()
		{
			return "PlainOutgoingMessage.txt";
		}
		
		
		protected void doAssertions(MockTxService service) throws Exception
		{
		}			
	}
	
	@Test
	public void testTrackMessage_iMFMessage_assertMessageNoTracked() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected void doAssertions(MockTxService service) throws Exception
			{
				
				assertEquals(0, service.txs.size());

			}			
		}.perform();
	}
	
	@Test
	public void testTrackMessage_nullParserAndNullTx_assertMessageNoTracked() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected void setupMocks() 
			{
				super.setupMocks();
				theMailet.txParser = null;
			}
			
			@Override
			protected void doAssertions(MockTxService service) throws Exception
			{
				
				assertEquals(0, service.txs.size());

			}			
		}.perform();
	}
	
	@Test
	public void testMonitorMessage_MDNMessage_assertMessageTracked() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected String getMessageToSend()
			{
				return "MDNMessage.txt";
			}
			
			@SuppressWarnings("deprecation")
			@Override
			protected void doAssertions(MockTxService service) throws Exception
			{
				
				assertEquals(1, service.txs.size());
				Tx tx = service.txs.iterator().next();
				assertEquals(TxMessageType.MDN, tx.getMsgType());
				
				MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource(getMessageToSend())));
				assertEquals(MailStandard.getHeader(msg, MailStandard.Headers.From).toLowerCase(Locale.getDefault()),
						tx.getDetail(TxDetailType.FROM).getDetailValue());
			}			
		}.perform();
	}
	
	@Test
	public void testMonitorMessage_DSNMessage_assertMessageTracked() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected String getMessageToSend()
			{
				return "DSNMessage.txt";
			}
			
			@SuppressWarnings("deprecation")
			@Override
			protected void doAssertions(MockTxService service) throws Exception
			{
				
				assertEquals(1, service.txs.size());
				Tx tx = service.txs.iterator().next();
				assertEquals(TxMessageType.DSN, tx.getMsgType());
				
				MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource(getMessageToSend())));
				assertEquals(MailStandard.getHeader(msg, MailStandard.Headers.From).toLowerCase(Locale.getDefault()),
						tx.getDetail(TxDetailType.FROM).getDetailValue());
			}			
		}.perform();
	}
}
