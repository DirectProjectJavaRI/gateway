package org.nhindirect.gateway.autoconfig;

import org.nhindirect.common.audit.Auditor;
import org.nhindirect.common.audit.AuditorFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AuditorAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	Auditor auditor()
	{
		return AuditorFactory.createAuditor(AuditorAutoConfiguration.class.getClassLoader());
	}
}
