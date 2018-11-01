package org.nhindirect.gateway.springconfig;

import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.gateway.smtp.dsn.impl.RejectedRecipientDSNCreator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DSNGeneratorConfig
{
	@Bean
	@ConditionalOnMissingBean
	public DSNCreator rejectedRecipientDSNCreator()
	{
		return new RejectedRecipientDSNCreator(null);
	}
}
