package org.nhindirect.gateway.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import org.nhindirect.stagent.SpringBaseTest;

public class GatewayState_getSetAttributesTest extends SpringBaseTest
{

	protected SmtpAgentFactory createAgentFactory()
	{
		return SmtpAgentFactory.getInstance(certService, bundleService, domainService, anchorService, 
				settingService, certPolService, null, keyStoreMgr);
	}
	
	@Test
	public void testGetSetAttributes_getSetAgent() throws Exception
	{
		GatewayState instance = GatewayState.getInstance();
		
		instance.setSmtpAgent(null);
		assertNull(instance.getSmtpAgent());
		
		SmtpAgent smtpAgent = mock(SmtpAgent.class);
		instance.setSmtpAgent(smtpAgent);
		assertNotNull(instance.getSmtpAgent());
		assertEquals(smtpAgent, instance.getSmtpAgent());
	}
	
	@Test
	public void testGetSetAttributes_getSetAgentConfig() throws Exception
	{
		GatewayState instance = GatewayState.getInstance();
		
		instance.setSmptAgentFactory(null);
		assertNull(instance.getSmtpAgentFactory());
		
		SmtpAgentFactory smtpAgentConfig = mock(SmtpAgentFactory.class);
		instance.setSmptAgentFactory(smtpAgentConfig);
		assertNotNull(instance.getSmtpAgentFactory());
		assertEquals(smtpAgentConfig, instance.getSmtpAgentFactory());
	}
	
	@Test
	public void testGetSetAttributes_getSetManagerInterval() throws Exception
	{
		GatewayState instance = GatewayState.getInstance();
		
		
		instance.setSettingsUpdateInterval(387278394);

		assertEquals(387278394, instance.getSettingsUpdateInterval());
	}
}
