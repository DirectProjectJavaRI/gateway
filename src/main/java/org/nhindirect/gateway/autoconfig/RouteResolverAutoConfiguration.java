package org.nhindirect.gateway.autoconfig;

import org.nhind.config.rest.AddressService;
import org.nhindirect.xd.routing.RoutingResolver;
import org.nhindirect.xd.routing.impl.RoutingResolverImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RouteResolverAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	RoutingResolver routingResolver(AddressService addressService) 
	{
		return new RoutingResolverImpl(addressService);
	}
}
