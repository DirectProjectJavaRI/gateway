package org.nhindirect.gateway.autoconfig;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nhind.config.rest.AnchorService;
import org.nhind.config.rest.CertPolicyService;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;
import org.nhind.config.rest.TrustBundleService;
import org.nhindirect.common.audit.Auditor;
import org.nhindirect.common.crypto.KeyStoreProtectionManager;
import org.nhindirect.gateway.smtp.SmtpAgentFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;

@AutoConfiguration
@Slf4j
public class SMTPGatewayFactoryAutoConfiguration
{
		
	@Bean
	@ConditionalOnMissingBean
	SmtpAgentFactory smtpAgentFactory(CertificateService certService, TrustBundleService bundleService, DomainService domainService, 
			AnchorService anchorService, SettingService settingService, CertPolicyService certPolService, Auditor auditor, KeyStoreProtectionManager keyStoreMgr,
			@Value("${direct.gateway.certificates.dns.servers:}") String dnsServers)
	{
		return SmtpAgentFactory.getInstance(certService, bundleService, domainService,
				anchorService, settingService, certPolService, auditor, keyStoreMgr, getDNSServers(dnsServers));
	}
	
	protected List<String> getDNSServers(String dnsServers)
	{
		final List<String> configedServers = (!StringUtils.isEmpty(dnsServers)) ? Arrays.asList(dnsServers.split(",")) : List.of();
		
		if (configedServers.isEmpty())
			log.info("No custom DNS servers set for Certificate lookup.  Will use OS default resolver server list");
		else
			log.info("Custom certificate DNS servers list provided: {}", configedServers);
		
		return configedServers;
	}
}
