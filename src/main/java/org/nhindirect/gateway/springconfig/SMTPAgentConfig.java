package org.nhindirect.gateway.springconfig;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.nhindirect.common.options.OptionsManager;
import org.nhindirect.common.options.OptionsParameter;
import org.nhindirect.gateway.smtp.GatewayState;
import org.nhindirect.gateway.smtp.SmtpAgent;
import org.nhindirect.gateway.smtp.SmtpAgentFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SMTPAgentConfig
{
	@Value("${direct.gateway.agent.useOutgoingPolicyForIncomingNotifications:true}")
	protected boolean useOutgoingPolicyForIncomingNotifications;
		
	@Value("${direct.gateway.agent.rejectOnTamper:false}")
	protected boolean rejectOnTamper;
	
	@Value("${direct.gateway.agent.jceProviderName:}")
	protected String jceProviderName;
	
	@Value("${direct.gateway.agent.jceSensitiveProviderName:}")
	protected String jceSenstiveProviderName;
		
	
	@Bean
	@ConditionalOnMissingBean
	public SmtpAgent smtpAgent(SmtpAgentFactory factory)
	{
		OptionsManager.getInstance().setOptionsParameter(
				new OptionsParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS, Boolean.toString(useOutgoingPolicyForIncomingNotifications)));
		
		OptionsManager.getInstance().setOptionsParameter(
				new OptionsParameter(OptionsParameter.REJECT_ON_ROUTING_TAMPER, Boolean.toString(rejectOnTamper)));		
		
		if (!StringUtils.isEmpty(jceProviderName))
			OptionsManager.getInstance().setOptionsParameter(
					new OptionsParameter(OptionsParameter.JCE_PROVIDER, jceProviderName));		
		
		if (!StringUtils.isEmpty(jceSenstiveProviderName))
			OptionsManager.getInstance().setOptionsParameter(
					new OptionsParameter(OptionsParameter.JCE_SENTITIVE_PROVIDER, jceSenstiveProviderName));			
		
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
