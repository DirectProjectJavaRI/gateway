package org.nhindirect.gateway.smtp.james.mailet;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.james.user.api.UsersRepository;
import org.apache.mailet.MailetConfig;
import org.nhindirect.gateway.smtp.dsn.impl.FailedDeliveryDSNCreator;

import junit.framework.TestCase;

public class TimelyAndReliableLocalDelivery_initTest extends TestCase
{
	protected MailetConfig getMailetConfig() throws Exception
	{
		Map<String,String> params = new HashMap<String, String>();
		
		return new MockMailetConfig(params, "TimelyAndReliableLocalDelivery");	
	}
	
	public void testInit_initSuccessful() throws Exception
	{
		TimelyAndReliableLocalDelivery mailet = new TimelyAndReliableLocalDelivery(mock(UsersRepository.class), mock(MailboxManager.class),
				mock(MetricFactory.class))
		{
			@Override
			protected LocalDelivery createLocalDeliveryClass()
			{
				return mock(LocalDelivery.class);
			}
		};
		
		mailet.init(getMailetConfig());
		
		assertNotNull(mailet.localDeliveryMailet);
		assertNotNull(mailet.txParser);
		assertNotNull(mailet.dsnCreator);
		assertTrue(mailet.dsnCreator instanceof FailedDeliveryDSNCreator);
	}
	
}
