package org.nhindirect.gateway.autoconfig;

import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.gateway.smtp.dsn.impl.FailedDeliveryDSNCreator;
import org.nhindirect.gateway.smtp.dsn.impl.RejectedRecipientDSNCreator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class DSNGeneratorAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	DSNCreator rejectedRecipientDSNCreator()
	{
		return new RejectedRecipientDSNCreator(null);
	}
	
	@Bean
	@ConditionalOnMissingBean
	DSNCreator failedDeliveryDSNCreator()
	{
		return new FailedDeliveryDSNCreator(null);
	}	
}
