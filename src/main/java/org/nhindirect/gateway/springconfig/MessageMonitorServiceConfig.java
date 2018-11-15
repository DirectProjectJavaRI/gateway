package org.nhindirect.gateway.springconfig;

import org.nhindirect.common.tx.TxDetailParser;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.impl.DefaultTxDetailParser;
import org.nhindirect.common.tx.impl.RESTTxServiceClient;
import org.nhindirect.common.tx.impl.feign.TxClient;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableFeignClients({"org.nhindirect.common.tx.impl.feign"})
@ImportAutoConfiguration({RibbonAutoConfiguration.class, FeignRibbonClientAutoConfiguration.class, FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
@PropertySource(value="classpath:properties/staMailet.properties", ignoreResourceNotFound=true)
public class MessageMonitorServiceConfig
{
    @Bean
    @ConditionalOnMissingBean
    public TxDetailParser txDetailParser()
    {
    	return new DefaultTxDetailParser();
    }	
	
	@Bean
	@ConditionalOnMissingBean
	public TxService monitorinService(TxClient txClient, TxDetailParser parser)
	{
		return new RESTTxServiceClient(txClient, parser);
	}
}
