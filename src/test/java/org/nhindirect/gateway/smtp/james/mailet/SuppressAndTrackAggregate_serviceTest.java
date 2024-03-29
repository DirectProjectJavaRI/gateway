package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.mailet.Mail;
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

public class SuppressAndTrackAggregate_serviceTest extends SpringBaseTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		SuppressAndTrackAggregate theMailet;
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			final Map<String,String> params = new HashMap<String, String>();			
			return new MockMailetConfig(params, "SuppressAndTrackAggregate");	
		}
		
		@Override
		protected void setupMocks() 
		{
			theMailet = new SuppressAndTrackAggregate()
			{
				public void init(MailetConfig newConfig) throws MessagingException
				{
					super.init(newConfig);
					
					tracker = new TrackIncomingNotification()
					{
						protected TxService createTxServices()
						{		
							return new MockTxService();							
						}		
						
					};
					suppessor = new NotificationSuppressor();
					
					suppessor.init(newConfig);
					tracker.init(newConfig);
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
			
			doAssertions(theMailet, theMessage);
		}

		protected String getMessageToSend()
		{
			return "PlainOutgoingMessage.txt";
		}
		
		
		protected void doAssertions(SuppressAndTrackAggregate service, Mail mail) throws Exception
		{
		}			
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
			protected void doAssertions(SuppressAndTrackAggregate service, Mail mail) throws Exception
			{
				MockTxService txService = (MockTxService)service.tracker.txService;
				
				assertEquals(1, txService.txs.size());
				Tx tx = txService.txs.iterator().next();
				assertEquals(TxMessageType.MDN, tx.getMsgType());
				
				MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource(getMessageToSend())));
				assertEquals(MailStandard.getHeader(msg, MailStandard.Headers.From).toLowerCase(Locale.getDefault()),
						tx.getDetail(TxDetailType.FROM).getDetailValue());
				
				// make sure it has not been ghosted
				assertEquals(Mail.TRANSPORT,  mail.getState());
			}			
		}.perform();
	}	
}
