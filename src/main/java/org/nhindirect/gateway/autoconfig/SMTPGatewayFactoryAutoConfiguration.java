package org.nhindirect.gateway.autoconfig;

import org.nhind.config.rest.AnchorService;
import org.nhind.config.rest.CertPolicyService;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;
import org.nhind.config.rest.TrustBundleService;
import org.nhindirect.common.audit.Auditor;
import org.nhindirect.common.crypto.KeyStoreProtectionManager;
import org.nhindirect.gateway.smtp.SmtpAgentFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SMTPGatewayFactoryAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	SmtpAgentFactory smtpAgentFactory(CertificateService certService, TrustBundleService bundleService, DomainService domainService, 
			AnchorService anchorService, SettingService settingService, CertPolicyService certPolService, Auditor auditor, KeyStoreProtectionManager keyStoreMgr)
	{
		return SmtpAgentFactory.getInstance(certService, bundleService, domainService,
				anchorService, settingService, certPolService, auditor, keyStoreMgr);
	}
}
