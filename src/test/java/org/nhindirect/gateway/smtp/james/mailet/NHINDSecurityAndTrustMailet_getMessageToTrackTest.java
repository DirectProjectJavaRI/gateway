package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


import org.apache.commons.io.IOUtils;
import org.apache.mailet.MailetConfig;

import org.nhindirect.common.mail.MailStandard;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.common.tx.model.TxMessageType;

import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.AddressSource;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.SpringBaseTest;

public class NHINDSecurityAndTrustMailet_getMessageToTrackTest extends SpringBaseTest
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
			theMailet = new NHINDSecurityAndTrustMailet()
			{
				@Override
				protected boolean isOutgoing(MimeMessage msg, NHINDAddress sender)
				{
					return isMessageOutgoing();
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
		
		@SuppressWarnings("deprecation")
		@Override
		protected void performInner() throws Exception
		{

			MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource(getMessageToSend())));
			NHINDAddress sender = new NHINDAddress((InternetAddress)msg.getFrom()[0]);
			
			
			final NHINDAddressCollection recipients = new NHINDAddressCollection();		


			final Address[] recipsAddr = msg.getAllRecipients();
			for (Address addr : recipsAddr)
			{
				
				recipients.add(new NHINDAddress(addr.toString(), (AddressSource)null));
			}
			
			doAssertions(theMailet.getTxToTrack(msg, sender, recipients));
		}
		
		protected String getMessageToSend()
		{
			return "PlainOutgoingMessage.txt";
		}
		
		protected boolean isMessageOutgoing()
		{
			return true;
		}
		
		protected void doAssertions(Tx tx) throws Exception
		{
		}			
	}
	
	@Test
	public void testMessageToTrackTest_nullParser_assertNullTx() throws Exception 
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
			protected void doAssertions(Tx tx) throws Exception
			{
				assertNull(tx);
			}				
		}.perform();
	}
	
	@Test
	public void testMessageToTrackTest_nonIMFMessage_assertMDNTx() throws Exception 
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
			protected void doAssertions(Tx tx) throws Exception
			{
				assertNotNull(tx);
				assertEquals(TxMessageType.MDN, tx.getMsgType());
				

				MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource(getMessageToSend())));
				assertEquals(MailStandard.getHeader(msg, MailStandard.Headers.From).toLowerCase(Locale.getDefault()),
						tx.getDetail(TxDetailType.FROM).getDetailValue());
			}				
		}.perform();
	}
	
	
	@Test
	public void testMessageToTrackTest_regularOutgoingMessage_assertTx() throws Exception 
	{
		new TestPlan() 
		{
			
			@Override
			@SuppressWarnings("deprecation")
			protected void doAssertions(Tx tx) throws Exception
			{
				assertNotNull(tx);
				assertEquals(TxMessageType.IMF, tx.getMsgType());
				
				MimeMessage msg = new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource(getMessageToSend())));
				assertEquals(MailStandard.getHeader(msg, MailStandard.Headers.From).toLowerCase(Locale.getDefault()),
						tx.getDetail(TxDetailType.FROM).getDetailValue());
			}				
		}.perform();
	}
}
