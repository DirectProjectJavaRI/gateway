package org.nhindirect.gateway.smtp;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.SpringBaseTest;

public class SmtpAgentFactory_CreateAgent_Test extends SpringBaseTest
{

	@Test
	public void testCreateDefaultAgent_ValidConfiguration() throws Exception
	{
		TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
		
		SmtpAgent agent = SmtpAgentFactory.getInstance(certService, bundleService, domainService, anchorService, settingService, 
				certPolService, null, keyStoreMgr).createSmtpAgent();
		
		assertNotNull(agent);
		assertNotNull(agent.getAgent());
	}	
	
	@Test
	public void testCreateDefaultAgent_InvalidConfiguration_AssertException() throws Exception
	{
		Assertions.assertThrows(SmtpAgentException.class, () -> 
		{
			SmtpAgentFactory.getInstance(certService, bundleService, domainService, anchorService, settingService, 
					certPolService, null, keyStoreMgr).createSmtpAgent();
		});
	}			
}
