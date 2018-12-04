package org.nhindirect.gateway.smtp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;


public class GatewayState_stopSettingManagerTest
{
	@Test
	public void testStopSettingsManager_managerNull_assertExepction() throws Exception
	{
		GatewayState instance = GatewayState.getInstance();
		
		if (instance.isAgentSettingManagerRunning())
			instance.stopAgentSettingsManager();
		
		boolean exceptionOccured = false;
		
		try
		{
			instance.stopAgentSettingsManager();
		}
		catch (IllegalStateException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}
	
	@Test
	public void testStopSettingsManager_assertStoppedSuccessful() throws Exception
	{
		GatewayState instance = GatewayState.getInstance();
		
		if (instance.isAgentSettingManagerRunning())
			instance.stopAgentSettingsManager();
		
		instance.setSmptAgentFactory(mock(SmtpAgentFactory.class));
		instance.setSmtpAgent(mock(SmtpAgent.class));
		
		instance.startAgentSettingsManager();
		
		assertTrue(instance.isAgentSettingManagerRunning());
		
		instance.stopAgentSettingsManager();
		assertFalse(instance.isAgentSettingManagerRunning());
	}
}
