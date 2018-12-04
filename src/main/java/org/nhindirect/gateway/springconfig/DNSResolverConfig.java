package org.nhindirect.gateway.springconfig;

import java.net.UnknownHostException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

@Configuration
public class DNSResolverConfig
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DNSResolverConfig.class);	
	
	@Value("${direct.gateway.remotedelivery.dns.lookup.timeout:3}")
	protected int dnsTimeout;
	
	@Value("${direct.gateway.remotedelivery.dns.lookup.retries:2}")
	protected int dnsRetries;
	
	@Value("${direct.gateway.remotedelivery.dns.servers:}")
	protected String dnsServers;
	
	
	@Bean
	public ExtendedResolver getDNSResolver() throws Exception
	{
		final ExtendedResolver extendedResolver = new ExtendedResolver();

		// remove all resolvers from default ExtendedResolver
		final Resolver[] resolvers = extendedResolver.getResolvers();
		if (!ArrayUtils.isEmpty(resolvers)) 
		{
			for (Resolver resolver : resolvers) 
			{
				extendedResolver.deleteResolver(resolver);
			}
		}
		
		if (!StringUtils.isEmpty(dnsServers)) 
		{
			for (String server : dnsServers.split(",")) 
			{
				// support for IP addresses instead of names
				server = server.replaceFirst("\\.$", "");

				try 
				{
					// create and add a SimpleResolver for each server
					SimpleResolver simpleResolver = new SimpleResolver(server);
					extendedResolver.addResolver(simpleResolver);
				} 
				catch (UnknownHostException e) 
				{
					LOGGER.warn("Unable to add resolver for " + server, e);
					continue;
				}
			}
		}
		
		extendedResolver.setRetries(dnsRetries);
		extendedResolver.setTimeout(dnsTimeout);
		
		return extendedResolver;
	}		
}
