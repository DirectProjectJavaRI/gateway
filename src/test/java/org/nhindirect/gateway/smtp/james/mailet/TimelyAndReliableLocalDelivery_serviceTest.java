package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;


import org.apache.james.mailbox.MailboxManager;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.james.user.api.UsersRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetConfig;
import org.nhindirect.common.mail.MDNStandard;
import org.nhindirect.common.mail.MailStandard;
import org.nhindirect.common.tx.TxUtil;
import org.nhindirect.common.tx.model.TxMessageType;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.parser.EntitySerializer;

public class TimelyAndReliableLocalDelivery_serviceTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		protected TimelyAndReliableLocalDelivery theMailet;
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			Map<String,String> params = new HashMap<String, String>();
			
			return new MockMailetConfig(params, "DirectBounce");	
		}
		
		@Override
		protected void setupMocks() 
		{
			theMailet = new TimelyAndReliableLocalDelivery(mock(UsersRepository.class), mock(MailboxManager.class),
					mock(MetricFactory.class))
			{
				@Override
				protected LocalDelivery createLocalDeliveryClass()
				{
					return mock(LocalDelivery.class);
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
			
			MimeMessage msg = applyCustomHeaders(EntitySerializer.Default.deserialize(originalMessage));
			
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
		
		protected MimeMessage applyCustomHeaders(MimeMessage msg) throws Exception
		{
			return msg;
		}
	}
	

	@Test
	public void testService_successfulDelivery_noReliableHeader_assertMDNNotCreated() throws Exception
	{
		new TestPlan() 
		{
			
			@Override
			protected void doAssertions(MockMailetContext context) throws Exception
			{
				assertEquals(0, context.getSentMessages().size());
				
			}			
		}.perform();
	}
	
	@Test
	public void testService_successfulDelivery_reliableHeader_assertMDNCreated() throws Exception
	{
		new TestPlan() 
		{
			@Override
			protected MimeMessage applyCustomHeaders(MimeMessage msg) throws Exception
			{
				msg.addHeader(MDNStandard.Headers.DispositionNotificationOptions, MDNStandard.DispositionOption_TimelyAndReliable);
				
				return msg;
			}
			
			@Override
			protected void doAssertions(MockMailetContext context) throws Exception
			{
				assertEquals(1, context.getSentMessages().size());
				
				MimeMessage msg = context.getSentMessages().iterator().next().getMessage();
				
				assertEquals(TxMessageType.MDN, TxUtil.getMessageType(msg));
				
				assertTrue(MDNStandard.getMDNField(msg, MDNStandard.Headers.Disposition).contains(MDNStandard.Disposition_Dispatched));
				
				final InternetHeaders headers = MDNStandard.getNotificationFieldsAsHeaders(msg);
				assertEquals("", headers.getHeader(MDNStandard.DispositionOption_TimelyAndReliable, ","));
				
			}			
		}.perform();
	}
	
	@Test
	public void testService_successfulDelivery_reliableHeader_nonIMFMessage_assertMDNNotCreated() throws Exception
	{
		new TestPlan() 
		{
			@Override
			protected String getMessageToSend()
			{
				return "MDNMessage.txt";
			}
			
			@Override
			protected MimeMessage applyCustomHeaders(MimeMessage msg) throws Exception
			{
				msg.addHeader(MDNStandard.Headers.DispositionNotificationOptions, MDNStandard.DispositionOption_TimelyAndReliable);
				
				return msg;
			}
			
			@Override
			protected void doAssertions(MockMailetContext context) throws Exception
			{
				assertEquals(0, context.getSentMessages().size());
				
			}			
		}.perform();
	}
	
	@Test
	public void testService_failedDelivery_assertDSNCreated() throws Exception
	{
		new TestPlan() 
		{
			@Override
			protected void setupMocks() 
			{
				theMailet = new TimelyAndReliableLocalDelivery(mock(UsersRepository.class), mock(MailboxManager.class),
						mock(MetricFactory.class))
				{
					protected LocalDelivery createLocalDeliveryClass()
					{
						LocalDelivery mailet = mock(LocalDelivery.class);
						try
						{
							doThrow(new RuntimeException()).when(mailet).service((Mail)any());
						} 
						catch (MessagingException e)
						{
							// TODO Auto-generated catch block
							
						}
						
						return mailet;
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
			protected void doAssertions(MockMailetContext context) throws Exception
			{
				assertEquals(1, context.getSentMessages().size());
				
				MimeMessage dsnMessage = context.getSentMessages().iterator().next().getMessage();
				assertEquals(TxMessageType.DSN, TxUtil.getMessageType(dsnMessage));
				
				String originalMessageString = TestUtils.readMessageResource(getMessageToSend());
				
				MimeMessage originalMsg = EntitySerializer.Default.deserialize(originalMessageString);
				
				NHINDAddress originalRecipAddress = new NHINDAddress(MailStandard.getHeader(originalMsg, MailStandard.Headers.To));
				NHINDAddress dsnFromAddress = new NHINDAddress(MailStandard.getHeader(dsnMessage, MailStandard.Headers.From));
				
				assertTrue(dsnFromAddress.getHost().toLowerCase(Locale.getDefault()).contains(originalRecipAddress.getHost().toLowerCase(Locale.getDefault())));
				
			}			
		}.perform();
	}
	
	@Test
	public void testService_failedDelivery_nonIMF_assertDSNNotCreated() throws Exception
	{
		new TestPlan() 
		{
			@Override
			protected void setupMocks() 
			{
				theMailet = new TimelyAndReliableLocalDelivery(mock(UsersRepository.class), mock(MailboxManager.class),
						mock(MetricFactory.class))
				{
					protected LocalDelivery createLocalDeliveryClass()
					{
						LocalDelivery mailet = mock(LocalDelivery.class);
						try
						{
							doThrow(new RuntimeException()).when(mailet).service((Mail)any());
						} 
						catch (MessagingException e)
						{
							// TODO Auto-generated catch block
							
						}
						
						return mailet;
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
