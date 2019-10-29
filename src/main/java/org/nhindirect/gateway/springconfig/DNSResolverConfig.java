package org.nhindirect.gateway.springconfig;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
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
		
		final List<String> servers = getDNSServers();
		
		for (String server : servers) 
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
		
		extendedResolver.setRetries(dnsRetries);
		extendedResolver.setTimeout(dnsTimeout);
		
		return extendedResolver;
	}		
	
	protected List<String> getDNSServers()
	{
		final String[] configedServers = (!StringUtils.isEmpty(dnsServers)) ? dnsServers.split(",") :
			ResolverConfig.getCurrentConfig().servers();
		
		return Arrays.asList(configedServers);
	}
}
