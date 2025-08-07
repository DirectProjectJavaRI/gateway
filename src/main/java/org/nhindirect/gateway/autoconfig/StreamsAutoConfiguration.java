package org.nhindirect.gateway.autoconfig;

import org.nhindirect.gateway.streams.STALastMileDeliverySource;
import org.nhindirect.gateway.streams.STAPostProcessSource;
import org.nhindirect.gateway.streams.STASource;
import org.nhindirect.gateway.streams.SmtpGatewayMessageSource;
import org.nhindirect.gateway.streams.SmtpRemoteDeliverySource;
import org.nhindirect.gateway.streams.XDRemoteDeliverySource;
import org.nhindirect.gateway.streams.processor.STAPostProcessProcessor;
import org.nhindirect.gateway.streams.processor.STAProcessor;
import org.nhindirect.gateway.streams.processor.SmtpGatewayMessageProcessor;
import org.nhindirect.gateway.streams.processor.SmtpRemoteDeliveryProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({SmtpGatewayMessageProcessor.class, SmtpRemoteDeliveryProcessor.class, 
	STAPostProcessProcessor.class, STAProcessor.class})
public class StreamsAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	SmtpGatewayMessageSource smtpGatewayMessageSource() {
		return new SmtpGatewayMessageSource();
	}
	
	@ConditionalOnMissingBean
	@Bean
	SmtpRemoteDeliverySource smtpRemoteDeliverySource() {
		return new SmtpRemoteDeliverySource();
	}
	
	@ConditionalOnMissingBean
	@Bean
	STALastMileDeliverySource staLastMileDeliverySource() {
		return new STALastMileDeliverySource();
	}
	
	@ConditionalOnMissingBean
	@Bean
	STAPostProcessSource staPostProcessSource() {
		return new STAPostProcessSource();
	}
	
	@ConditionalOnMissingBean
	@Bean
	STASource staSource() {
		return new STASource();
	}
	
	@ConditionalOnMissingBean
	@Bean
	XDRemoteDeliverySource xdRemoteDeliverySource() {
		return new XDRemoteDeliverySource();
	}
}
