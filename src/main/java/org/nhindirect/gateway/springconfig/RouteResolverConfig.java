package org.nhindirect.gateway.springconfig;

import org.nhind.config.rest.AddressService;
import org.nhindirect.xd.routing.RoutingResolver;
import org.nhindirect.xd.routing.impl.RoutingResolverImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteResolverConfig
{
	@Bean
	@ConditionalOnMissingBean
	public RoutingResolver routingResolver(AddressService addressService) 
	{
		return new RoutingResolverImpl(addressService);
	}
}
