package org.nhindirect.gateway.autoconfig;

import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.impl.RESTTxServiceClient;
import org.nhindirect.common.tx.impl.exchange.TxClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@PropertySource(value="classpath:properties/staMailet.properties", ignoreResourceNotFound=true)
public class MessageMonitorServiceAutoConfiguration
{	
	@Bean
	@ConditionalOnMissingBean
	TxService monitorinService(TxClient txClient, TxDetailParser parser)
	{
		return new RESTTxServiceClient(txClient, parser);
	}
}
