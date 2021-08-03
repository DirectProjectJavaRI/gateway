package org.nhindirect.gateway.smtp;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class GatewayState_startSettingsManagerTest
{
	@Test
	public void testStartSettingsManager_nullAgent_assertException() throws Exception
	{
		
		GatewayState instance = GatewayState.getInstance();
		
		if (instance.isAgentSettingManagerRunning())
			instance.stopAgentSettingsManager();
		
		instance.setSmptAgentFactory(mock(SmtpAgentFactory.class));
		instance.setSmtpAgent(null);
		
		boolean exceptionOccured = false;
		
		try
		{
			instance.startAgentSettingsManager();
		}
		catch (IllegalStateException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}
	
	@Test
	public void testStartSettingsManager_nullAgentConfig_assertException() throws Exception
	{
		
		GatewayState instance = GatewayState.getInstance();
		
		if (instance.isAgentSettingManagerRunning())
			instance.stopAgentSettingsManager();
		
		instance.setSmptAgentFactory(null);
		instance.setSmtpAgent(mock(SmtpAgent.class));
		
		boolean exceptionOccured = false;
		
		try
		{
			instance.startAgentSettingsManager();
		}
		catch (IllegalStateException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}
	
	@Test
	public void testStartSettingsManager_managerAlreadyRunning_assertException() throws Exception
	{
		GatewayState instance = GatewayState.getInstance();
		
		if (instance.isAgentSettingManagerRunning())
			instance.stopAgentSettingsManager();
		
		instance.setSmptAgentFactory(mock(SmtpAgentFactory.class));
		instance.setSmtpAgent(mock(SmtpAgent.class));
		
		instance.startAgentSettingsManager();
		
		boolean exceptionOccured = false;
		try
		{
			instance.startAgentSettingsManager();
		}
		catch (IllegalStateException e)
		{
			exceptionOccured = true;
		}
		
		assertTrue(exceptionOccured);
	}
	
	@Test
	public void testStartSettingsManager_assertSuccessfulStart() throws Exception
	{
		GatewayState instance = GatewayState.getInstance();
		
		if (instance.isAgentSettingManagerRunning())
			instance.stopAgentSettingsManager();
		
		instance.setSmptAgentFactory(mock(SmtpAgentFactory.class));
		instance.setSmtpAgent(mock(SmtpAgent.class));
		
		instance.startAgentSettingsManager();
		
		assertTrue(instance.isAgentSettingManagerRunning());
	}
}
