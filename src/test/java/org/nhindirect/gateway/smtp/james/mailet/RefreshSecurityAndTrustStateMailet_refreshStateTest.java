package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;
import org.nhindirect.gateway.smtp.GatewayState;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.SpringBaseTest;

public class RefreshSecurityAndTrustStateMailet_refreshStateTest extends SpringBaseTest
{
	
	protected Mailet getMailet()  throws Exception
	{
		Mailet retVal = null;
		TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
		Map<String,String> params = new HashMap<String, String>();
		
		retVal = new NHINDSecurityAndTrustMailet();
		
		MailetConfig mailetConfig = new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");
		
		retVal.init(mailetConfig);
		
		return retVal;
	}
	
	@Test
	public void testRefreshWithUpdateManagerAlreadyRunning() throws Exception
	{
		getMailet();
		
		assertTrue(GatewayState.getInstance().isAgentSettingManagerRunning());
		
		RefreshSecurityAndTrustStateMailet refreshMailet = new RefreshSecurityAndTrustStateMailet();
		
		refreshMailet.service(new MockMail(null));
		
		assertTrue(GatewayState.getInstance().isAgentSettingManagerRunning());
	}
	
	public void testRefreshWithUpdateManagerNotRunning() throws Exception
	{
		getMailet();
		
		if (GatewayState.getInstance().isAgentSettingManagerRunning())
			GatewayState.getInstance().stopAgentSettingsManager();
		
		assertFalse(GatewayState.getInstance().isAgentSettingManagerRunning());
		
		RefreshSecurityAndTrustStateMailet refreshMailet = new RefreshSecurityAndTrustStateMailet();
		
		refreshMailet.service(new MockMail(null));
		
		assertTrue(GatewayState.getInstance().isAgentSettingManagerRunning());
	}
}
