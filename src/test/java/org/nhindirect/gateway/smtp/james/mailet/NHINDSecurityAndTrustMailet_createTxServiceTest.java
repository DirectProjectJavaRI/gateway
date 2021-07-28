package org.nhindirect.gateway.smtp.james.mailet;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;


import org.apache.mailet.MailetConfig;

import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.impl.NoOpTxServiceClient;
import org.nhindirect.common.tx.impl.RESTTxServiceClient;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.SpringBaseTest;

public class NHINDSecurityAndTrustMailet_createTxServiceTest extends SpringBaseTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
			Map<String,String> params = new HashMap<String, String>();

			
			return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
		}
		
		@Override
		protected void performInner() throws Exception
		{
			NHINDSecurityAndTrustMailet theMailet = new NHINDSecurityAndTrustMailet();

			MailetConfig config = getMailetConfig();
			
			theMailet.init(config);
			doAssertions(theMailet.txService);
		}
		
		protected String getMessageMonitoringServiceURL()
		{
			return "";
		}
		
		protected void doAssertions(TxService txService) throws Exception
		{
		}			
	}
	
	@Test
	public void testCreateDefaultServiceModules_noAppContext_assertNoOpMonitoringService() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected void performInner() throws Exception
			{
				final NHINDSecurityAndTrustMailet theMailet = new NHINDSecurityAndTrustMailet();
				doAssertions(theMailet.createTxServices());
			}
			
			@Override
			protected void doAssertions(TxService txService) 
			{

				assertTrue(txService instanceof NoOpTxServiceClient);
			}				
		}.perform();
	}
	
	@Test
	public void testCreateDefaultServiceModules_appContextAvailable_assertDefaultTxService() throws Exception 
	{
		new TestPlan() 
		{

			
			@Override
			protected void doAssertions(TxService txService) 
			{

				assertTrue(txService instanceof RESTTxServiceClient);
			}				
		}.perform();
	}
}
