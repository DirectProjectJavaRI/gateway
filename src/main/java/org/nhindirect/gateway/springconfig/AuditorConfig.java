package org.nhindirect.gateway.springconfig;

import org.nhindirect.common.audit.Auditor;
import org.nhindirect.common.audit.AuditorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditorConfig
{
	@Bean
	@ConditionalOnMissingBean
	public Auditor auditor()
	{
		return AuditorFactory.createAuditor(AuditorConfig.class.getClassLoader());
	}
}
