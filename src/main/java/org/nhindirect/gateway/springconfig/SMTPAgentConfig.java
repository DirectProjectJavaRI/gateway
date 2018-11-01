package org.nhindirect.gateway.springconfig;

import javax.annotation.PreDestroy;

import org.nhindirect.gateway.smtp.GatewayState;
import org.nhindirect.gateway.smtp.SmtpAgent;
import org.nhindirect.gateway.smtp.SmtpAgentFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SMTPAgentConfig
{
	@Bean
	@ConditionalOnMissingBean
	public SmtpAgent smtpAgent(SmtpAgentFactory factory)
	{
		final SmtpAgent agent = factory.createSmtpAgent();
		
		final GatewayState gwState = GatewayState.getInstance();
		
		if (gwState.isAgentSettingManagerRunning())
			gwState.stopAgentSettingsManager();
		
		gwState.setSmtpAgent(agent);
		gwState.setSmptAgentFactory(factory);
		gwState.startAgentSettingsManager();
		
		return agent;
	}
	
	@PreDestroy
	public void destroy() throws Exception
	{
	    final GatewayState state = GatewayState.getInstance();
	    state.lockForUpdating();
	    try 
	    {
	       if (state.isAgentSettingManagerRunning())
	          state.stopAgentSettingsManager();
	    } 
	    finally 
	    {
	      state.unlockFromUpdating();
	    }
	}
}
