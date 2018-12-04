package org.nhindirect.gateway.smtp.james.mailet;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetConfig;
import org.junit.Test;
import org.nhindirect.common.rest.exceptions.ServiceException;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.impl.NoOpTxServiceClient;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.SpringBaseTest;


public class NotificationSupressor_suppressMessageTest extends SpringBaseTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		NotificationSuppressor theMailet;
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
			Map<String,String> params = new HashMap<String, String>();
			
			params.put(SecurityAndTrustMailetOptions.CONSUME_MND_PROCESSED_PARAM, getConsumeMDNSetting());
			
			return new MockMailetConfig(params, "NotificationSupressor");	
		}
		
		@Override
		protected void setupMocks() 
		{
			theMailet = new NotificationSuppressor()
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
			MimeMessage msg = getMessageToSuppress();
			
			MockMail mail = new MockMail(msg);
			theMailet.service(mail);
			
			doAssertions(mail.getState().equals(Mail.GHOST));
		}
		
		protected String getConsumeMDNSetting()
		{
			return "true";
		}
		
		
		protected abstract MimeMessage getMessageToSuppress() throws Exception;
		
		protected boolean isOutgoing()
		{
			return true;
		}
		
		protected void doAssertions(boolean consumeMessage) throws Exception
		{
			
		}			
	}
	
	@Test
	public void testConsumeMessage_nonNotificationMessage_assertFalse() throws Exception 
	{
		new TestPlan() 
		{
			
			@SuppressWarnings("deprecation")
			@Override
			protected MimeMessage getMessageToSuppress() throws Exception
			{
				return new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("PlainOutgoingMessage.txt")));
			}
			
			@Override
			protected void doAssertions(boolean consumeMessage) throws Exception
			{
				assertFalse(consumeMessage);
			}						
		}.perform();
	}
	
	@Test
	public void testConsumeMessage_noTxService_assertFalse() throws Exception 
	{
		new TestPlan() 
		{
			
			@Override
			protected void setupMocks() 
			{
				super.setupMocks();
				this.theMailet.txService = null;
			}
			
			@SuppressWarnings("deprecation")
			@Override
			protected MimeMessage getMessageToSuppress() throws Exception
			{
				return new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("PlainOutgoingMessage.txt")));
			}
			
			@Override
			protected void doAssertions(boolean consumeMessage) throws Exception
			{
				assertFalse(consumeMessage);
			}						
		}.perform();
	}
	
	@Test
	public void testConsumeMessage_consumeMDNFlagSet_MDNProccessedMessage_assertTrue() throws Exception 
	{
		new TestPlan() 
		{
			
			@SuppressWarnings("deprecation")
			@Override
			protected MimeMessage getMessageToSuppress() throws Exception
			{
				return new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("MDNMessage.txt")));
			}
			
			@Override
			protected void doAssertions(boolean consumeMessage) throws Exception
			{
				assertTrue(consumeMessage);
			}						
		}.perform();
		
	}
	
	@Test
	public void testConsumeMessage_consumeMDNFlagNotSet_MDNProccessedMessage_assertFalse() throws Exception 
	{
		new TestPlan() 
		{
			
			protected String getConsumeMDNSetting()
			{
				return "false";
			}
			
			@SuppressWarnings("deprecation")
			@Override
			protected MimeMessage getMessageToSuppress() throws Exception
			{
				return new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("MDNMessage.txt")));
			}
			
			@Override
			protected void doAssertions(boolean consumeMessage) throws Exception
			{
				assertFalse(consumeMessage);
			}						
		}.perform();
	}
	
	@Test
	public void testConsumeMessage_consumeMDNFlagSet_dispositionEmpty_assertFalse() throws Exception 
	{
		new TestPlan() 
		{
			
			@SuppressWarnings("deprecation")
			@Override
			protected MimeMessage getMessageToSuppress() throws Exception
			{
				return new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("PlainOutgoingMessage.txt")));
			}
			
			@Override
			protected void doAssertions(boolean consumeMessage) throws Exception
			{
				assertFalse(consumeMessage);
			}						
		}.perform();
	}
	
	@Test
	public void testConsumeMessage_consumeMDNFlagSet_dispositionProcessed_assertFalse() throws Exception 
	{
		new TestPlan() 
		{
			
			@SuppressWarnings("deprecation")
			@Override
			protected MimeMessage getMessageToSuppress() throws Exception
			{
				return new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("MDNDispatchedMessage.txt")));
			}
			
			@Override
			protected void doAssertions(boolean consumeMessage) throws Exception
			{
				assertFalse(consumeMessage);
			}						
		}.perform();
	}
	
	@Test
	public void testConsumeMessage_txServiceReturnsTrue_assertTrue() throws Exception 
	{
		new TestPlan() 
		{
			
			@Override
			protected void setupMocks() 
			{
				super.setupMocks();
				final TxService service = mock(TxService.class);
				try
				{
					when(service.suppressNotification((Tx)any())).thenReturn(true);
				}
				catch (ServiceException e){
				
				}
				theMailet.txService = service;
			}
			
			@SuppressWarnings("deprecation")
			@Override
			protected MimeMessage getMessageToSuppress() throws Exception
			{
				return new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("MDNDispatchedMessage.txt")));
			}
			
			@Override
			protected void doAssertions(boolean consumeMessage) throws Exception
			{
				assertTrue(consumeMessage);
			}						
		}.perform();
	}
	
	@Test
	public void testConsumeMessage_exceptionInService_assertFalse() throws Exception 
	{
		new TestPlan() 
		{
			
			@Override
			protected void setupMocks() 
			{
				super.setupMocks();
				final TxService service = mock(TxService.class);
				try
				{
					when(service.suppressNotification((Tx)any())).thenThrow(new ServiceException());
				}
				catch (ServiceException e){
				
				}
				theMailet.txService = service;
			}
			
			@SuppressWarnings("deprecation")
			@Override
			protected MimeMessage getMessageToSuppress() throws Exception
			{
				return new MimeMessage(null, IOUtils.toInputStream(TestUtils.readMessageResource("MDNDispatchedMessage.txt")));
			}
			
			@Override
			protected void doAssertions(boolean consumeMessage) throws Exception
			{
				assertFalse(consumeMessage);
			}						
		}.perform();
	}
}
