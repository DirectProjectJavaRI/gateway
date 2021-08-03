package org.nhindirect.gateway.smtp.james.mailet;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;


import org.apache.mailet.MailetConfig;
import org.nhindirect.common.tx.impl.DefaultTxDetailParser;
import org.nhindirect.gateway.testutils.BaseTestPlan;


public class TrackIncomingNotification_initializationTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			Map<String,String> params = new HashMap<String, String>();
			
			
			return new MockMailetConfig(params, "TrackIncomingNotification");	
		}
		
		@Override
		protected void performInner() throws Exception
		{
			TrackIncomingNotification theMailet = new TrackIncomingNotification();

			MailetConfig config = getMailetConfig();
			
			theMailet.init(config);
			doAssertions(theMailet);
		}


		protected void doAssertions(TrackIncomingNotification notif) throws Exception
		{

		}		
		
	}
	
	@Test
	public void testInitialization_emptyMonitorURL() throws Exception 
	{
		new TestPlan() 
		{
			protected void doAssertions(TrackIncomingNotification notif) throws Exception
			{
				assertNotNull(notif.txParser);
				assertNotNull(notif.txService);
				assertTrue(notif.txParser instanceof DefaultTxDetailParser);
			}	
		}.perform();
	}	
	
	@Test
	public void testInitialization_nullMonitorURL() throws Exception 
	{
		new TestPlan() 
		{
			
			protected void doAssertions(TrackIncomingNotification notif) throws Exception
			{
				assertNotNull(notif.txParser);
				assertNotNull(notif.txService);
				assertTrue(notif.txParser instanceof DefaultTxDetailParser);
			}	
		}.perform();
	}	
	
	@Test
	public void testInitialization_valueMonitorURL() throws Exception 
	{
		new TestPlan() 
		{
			
			protected void doAssertions(TrackIncomingNotification notif) throws Exception
			{
				assertNotNull(notif.txParser);
				assertNotNull(notif.txService);
				assertTrue(notif.txParser instanceof DefaultTxDetailParser);
			}	
		}.perform();
	}
	
}
