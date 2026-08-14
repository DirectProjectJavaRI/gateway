package org.nhindirect.gateway.autoconfig;

import org.nhind.config.rest.AddressService;
import org.nhind.config.rest.AnchorService;
import org.nhind.config.rest.CertPolicyService;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;
import org.nhind.config.rest.TrustBundleService;
import org.nhind.config.rest.exchange.AddressClient;
import org.nhind.config.rest.exchange.AnchorClient;
import org.nhind.config.rest.exchange.CertificateClient;
import org.nhind.config.rest.exchange.CertificatePolicyClient;
import org.nhind.config.rest.exchange.DomainClient;
import org.nhind.config.rest.exchange.SettingClient;
import org.nhind.config.rest.exchange.TrustBundleClient;
import org.nhind.config.rest.impl.DefaultAddressService;
import org.nhind.config.rest.impl.DefaultAnchorService;
import org.nhind.config.rest.impl.DefaultCertPolicyService;
import org.nhind.config.rest.impl.DefaultCertificateService;
import org.nhind.config.rest.impl.DefaultDomainService;
import org.nhind.config.rest.impl.DefaultSettingService;
import org.nhind.config.rest.impl.DefaultTrustBundleService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@AutoConfiguration
@PropertySource(value="classpath:properties/staMailet.properties", ignoreResourceNotFound=true)
public class ConfigServiceClientAutoConfiguration
{	
	
	@Bean
	@ConditionalOnMissingBean
	CertificateService certificateService(CertificateClient certClient)
	{
		return new DefaultCertificateService(certClient);
	}
	
	@Bean
	@ConditionalOnMissingBean
	TrustBundleService trustBundleService(TrustBundleClient bundleClient)
	{
		return new DefaultTrustBundleService(bundleClient);
	}	
	
	@Bean
	@ConditionalOnMissingBean
	DomainService domainService(DomainClient domainClient)
	{
		return new DefaultDomainService(domainClient);
	}	
	
	@Bean
	@ConditionalOnMissingBean
	AnchorService anchorService(AnchorClient anchorClient)
	{
		return new DefaultAnchorService(anchorClient);
	}	
	
	@Bean
	@ConditionalOnMissingBean
	CertPolicyService certPolicyService(CertificatePolicyClient polClient)
	{
		return new DefaultCertPolicyService(polClient);
	}	
	
	@Bean
	@ConditionalOnMissingBean
	SettingService settingService(SettingClient settingClient)
	{
		return new DefaultSettingService(settingClient);
	}	
	
	@Bean
	@ConditionalOnMissingBean
	AddressService addressService(AddressClient addressClient)
	{
		return new DefaultAddressService(addressClient);
	}	
	
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() 
    {
       return new PropertySourcesPlaceholderConfigurer();
    }
}
