package org.nhindirect.gateway.springconfig;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SimpleResolver;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DNSResolverConfig
{

	@Value("${direct.gateway.remotedelivery.dns.lookup.timeout:3}")
	protected int dnsTimeout;
	
	@Value("${direct.gateway.remotedelivery.dns.lookup.retries:2}")
	protected int dnsRetries;
	
	@Value("${direct.gateway.remotedelivery.dns.servers:}")
	protected String dnsServers;
	
	
	@Bean
	public ExtendedResolver getDNSResolver() throws Exception
	{
		ExtendedResolver extendedResolver = null;
		try
		{
			extendedResolver = new ExtendedResolver();
	
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
					log.warn("Unable to add resolver for " + server, e);
					continue;
				}
			}
			
			extendedResolver.setRetries(dnsRetries);
			extendedResolver.setTimeout(Duration.ofSeconds(dnsTimeout));
			
			
		}
		catch (NoSuchMethodError e)
		{
			log.warn("Extended resolver could not be created.  This is likely due to different version of DNS Java.");
		}
		
		return extendedResolver;
	}		
	
	protected List<String> getDNSServers()
	{
		final List<String> configedServers = (!StringUtils.isEmpty(dnsServers)) ? Arrays.asList(dnsServers.split(",")) :
			ResolverConfig.getCurrentConfig().servers()
			  .stream().map(addr -> addr.getHostString()).collect(Collectors.toList());
		
		return configedServers;
	}
}
