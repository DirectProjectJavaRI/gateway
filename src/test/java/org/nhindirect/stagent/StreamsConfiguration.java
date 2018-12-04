package org.nhindirect.stagent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;


import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


import static org.mockito.Matchers.any;

import org.apache.commons.io.FileUtils;

import org.nhind.config.rest.AnchorService;
import org.nhind.config.rest.CertPolicyService;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;
import org.nhind.config.rest.TrustBundleService;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.gateway.springconfig.AuditorConfig;
import org.nhindirect.gateway.springconfig.DNSResolverConfig;
import org.nhindirect.gateway.springconfig.DSNGeneratorConfig;
import org.nhindirect.gateway.springconfig.KeyStoreProtectionMgrConfig;
import org.nhindirect.gateway.springconfig.MessageMonitorServiceConfig;
import org.nhindirect.gateway.springconfig.RouteResolverConfig;
import org.nhindirect.gateway.springconfig.SMTPAgentConfig;
import org.nhindirect.gateway.springconfig.SMTPGatewayFactoryConfig;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@ComponentScan({"org.nhindirect.config", "org.nhind.config", "org.nhindirect.gateway.streams"})
@EnableFeignClients({"org.nhind.config.rest.feign"})
@Import({DSNGeneratorConfig.class, MessageMonitorServiceConfig.class, AuditorConfig.class, 
	KeyStoreProtectionMgrConfig.class, SMTPAgentConfig.class, SMTPGatewayFactoryConfig.class, DNSResolverConfig.class, 
	RouteResolverConfig.class})
@Configuration
@Profile("streams")
public class StreamsConfiguration
{
    
    @Bean
    @Primary
    public SettingService mockSettingService()
    {
    	return mock(SettingService.class);
    }
    
    @Bean
    @Primary
    public CertificateService mockCertService() throws Exception
    {
    	CertificateService certService = mock(CertificateService.class);
    	
    	Collection<Certificate> certs = new ArrayList<>();
    	Certificate cert = new Certificate();
    	cert.setData(FileUtils.readFileToByteArray(new File("./src/test/resources/certs/dualUse.p12")));
    	cert.setPrivateKey(true);
    	certs.add(cert);
    	when(certService.getCertificatesByOwner(eq("Cerner.com"))).
    		thenReturn(certs);
    	
    	
    	certs = new ArrayList<>();
    	cert = new Certificate();
    	cert.setData(FileUtils.readFileToByteArray(new File("./src/test/resources/certs/cernerdemos.der")));
    	cert.setPrivateKey(true);
    	certs.add(cert);
    	when(certService.getCertificatesByOwner(eq("starugh-stateline.com"))).
    		thenReturn(certs);
    	
    	return certService;
    }
    
    @Bean
    @Primary
    public DomainService mockDomainService() throws Exception
    {
    	final Domain domain = new Domain();
    	domain.setDomainName("cerner.com");
    	domain.setStatus(EntityStatus.ENABLED);
    	
    	final DomainService domainService = mock(DomainService.class);
    	when(domainService.searchDomains((String)any(), (EntityStatus)any())).thenReturn(Arrays.asList(domain));
    	
    	
    	return domainService;
    }   
    
    @Bean
    @Primary
    public TrustBundleService mockBundleService()
    {
    	return mock(TrustBundleService.class);
    }  
    
    @Bean
    @Primary
    public CertPolicyService mockCertPolicyService()
    {
    	return mock(CertPolicyService.class);
    }   
  
    
    @Bean
    @Primary
    public AnchorService mockAnchorService() throws Exception
    {
    	final AnchorService anchorService = mock(AnchorService.class);
    	
    	Collection<Anchor> anchors = new ArrayList<>();
    	Anchor anchor = new Anchor();
    	anchor.setCertificateData(FileUtils.readFileToByteArray(new File("./src/test/resources/certs/dualUse.der")));
    	anchor.setOwner("cerner.com");
    	anchor.setIncoming(true);
    	anchor.setOutgoing(true);
    	anchors.add(anchor);
    	
    	anchor = new Anchor();
    	anchor.setCertificateData(FileUtils.readFileToByteArray(new File("./src/test/resources/certs/cernerdemos.der")));
    	anchor.setOwner("starugh-stateline.com");
    	anchors.add(anchor);
    	anchor.setIncoming(true);
    	anchor.setOutgoing(true);   
    	
    	when(anchorService.getAnchorsForOwner(eq("cerner.com"), eq(false), eq(false), (String)any())).thenReturn(anchors);

    	return anchorService;
    }       
}
