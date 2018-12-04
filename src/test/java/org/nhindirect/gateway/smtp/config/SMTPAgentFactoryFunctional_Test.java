package org.nhindirect.gateway.smtp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Iterator;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;

import org.junit.Test;
import org.nhindirect.common.crypto.CryptoExtensions;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.model.CertPolicyGroupUse;
import org.nhindirect.config.model.CertPolicyUse;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.gateway.smtp.SmtpAgent;
import org.nhindirect.gateway.smtp.SmtpAgentFactory;
import org.nhindirect.gateway.smtp.SmtpAgentSettings;
import org.nhindirect.gateway.smtp.config.cert.impl.ConfigServiceRESTCertificateStore;

import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.policy.PolicyExpression;
import org.nhindirect.policy.PolicyLexicon;
import org.nhindirect.stagent.DefaultNHINDAgent;
import org.nhindirect.stagent.IncomingMessage;
import org.nhindirect.stagent.MockAuditor;
import org.nhindirect.stagent.MutableAgent;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;
import org.nhindirect.stagent.NHINDAgentAccessor;
import org.nhindirect.stagent.OutgoingMessage;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.cert.CertCacheFactory;
import org.nhindirect.stagent.cert.CertificateResolver;
import org.nhindirect.stagent.cert.impl.DNSCertificateStore;
import org.nhindirect.stagent.cert.impl.KeyStoreCertificateStore;
import org.nhindirect.stagent.cert.impl.LDAPCertificateStore;
import org.nhindirect.stagent.cert.impl.TrustAnchorCertificateStore;
import org.nhindirect.stagent.mail.Message;
import org.nhindirect.stagent.policy.PolicyResolver;
import org.nhindirect.stagent.trust.TrustAnchorResolver;


public class SMTPAgentFactoryFunctional_Test extends SpringBaseTest 
{
	private static final String certBasePath = "src/test/resources/certs/";
		
	protected String filePrefix;
	
	static
	{
		CryptoExtensions.registerJCEProviders();
	}
	
	/**
     * Initialize the servers- LDAP and HTTP.
     */
	@Override
	public void setUp() throws Exception
	{
		// check for Windows... it doens't like file://<drive>... turns it into FTP
		File file = new File("./src/test/resources/bundles/testBundle.p7b");
		if (file.getAbsolutePath().contains(":/"))
			filePrefix = "file:///";
		else
			filePrefix = "file:///";
		
		CertCacheFactory.getInstance().flushAll();
	
		super.setUp();	
	}

    
	abstract class TestPlan extends BaseTestPlan 
    {
        @Override
        protected void performInner() throws Exception 
        {     
            removeTestFiles();
            cleanConfig();
            addConfiguration();
                      
            SmtpAgent agent = SmtpAgentFactory.getInstance(certService, bundleService, domainService, 
            		anchorService, settingService, certPolService, new MockAuditor(), keyStoreMgr).createSmtpAgent();
            doAssertions(agent);
            removeTestFiles();
        }  
        
        protected void addPublicCertificates() throws Exception
        {
        	// default uses DNS
        }
        
        protected abstract void addPrivateCertificates() throws Exception;
        
        protected void cleanConfig() throws Exception
        {
     	        	

        }

        
        protected void addConfiguration() throws Exception
        {
        	addDomains();
        	
        	addTrustAnchors();
        	
        	addPublicCertificates();
        	
        	addPrivateCertificates();  
        	
        	addSettings();
        }
        
        protected void addSettings() throws Exception
        {
        	// just use default settings
        }
        
        protected void addTrustAnchors() throws Exception
        {
        	Anchor anchor = new Anchor();
        	anchor.setCertificateData(getCertificateFileData("cacert.der"));
        	anchor.setOwner("cerner.com");
        	anchor.setIncoming(true);
        	anchor.setOutgoing(true);
        	anchor.setStatus(EntityStatus.ENABLED);
        	anchorService.addAnchor(anchor);
        	
        	anchor = new Anchor();
        	anchor.setCertificateData(getCertificateFileData("cacert.der"));
        	anchor.setOwner("securehealthemail.com");
        	anchor.setIncoming(true);
        	anchor.setOutgoing(true);
        	anchor.setStatus(EntityStatus.ENABLED);        	
        	anchorService.addAnchor(anchor);
        }
        
        protected void addDomains() throws Exception
        {
        	Domain dom = new Domain();
        	dom.setDomainName("cerner.com");
        	domainService.addDomain(dom);
        	
        	dom = new Domain();
        	dom.setDomainName("securehealthemail.com");
        	domainService.addDomain(dom);
        }
        
        protected void removeTestFiles()
        {
            removeFile("LDAPPrivateCertStore");
            removeFile("LDAPTrustAnchorStore");
            removeFile("LdapCacheStore");
            removeFile("DNSCacheStore");
            removeFile("WSPrivCacheStore");
            removeFile("PublicStoreKeyFile");
            removeFile("WSPublicCacheStore");
            removeFile("PublicLDAPCacheStore");
        }
        
        protected void removeFile(String filename){
            File delete = new File(filename);
            delete.delete();
        }
        
        
        protected void doAssertions(SmtpAgent agent) throws Exception
        {
        	assertNotNull(agent);
        	
        	DefaultNHINDAgent nAgent = ((DefaultNHINDAgent)agent.getAgent());
        	TrustAnchorResolver trustResolver = nAgent.getTrustAnchors();
            assertNotNull(trustResolver);             
            assertAnchors(trustResolver.getIncomingAnchors());  
            SmtpAgentSettings settings = agent.getSmtpAgentSettings();
            assertNotNull(settings);
            
        }
        
        protected void assertAnchors(CertificateResolver anchors) throws Exception
        {
        	
        }
        
        protected int createNumberOfDomains(){
            return 0;
        }
        
        protected int createNumberOfCerts(){
            return 0;
        }  
        
        
        protected void assertDomainConfig(SmtpAgent agent){
            Collection<String>  domains = agent.getAgent().getDomains();
            assertNotNull(domains);
            boolean cernerConfigured = false;
            boolean secureHealthconfigured = false;
            assertEquals(createNumberOfDomains(), domains.size());            
            cernerConfigured = false;
            secureHealthconfigured = false;
            for (String domain : domains)
            {
                if (domain.equalsIgnoreCase("cerner.com"))
                    cernerConfigured = true;
                else if (domain.equalsIgnoreCase("securehealthemail.com"))
                    secureHealthconfigured = true; 
            }                
            assertTrue(cernerConfigured);
            assertTrue(secureHealthconfigured);
        }  
    }
	
	abstract class MultiDomainTestPlan extends TestPlan {
	    @Override
        protected void assertAnchors(CertificateResolver anchors) throws Exception{
	    	assertNotNull(anchors);
        }

	    @Override
        protected void assertDomainConfig(SmtpAgent agent){
            Collection<String>  domains = agent.getAgent().getDomains();
            assertNotNull(domains);
            boolean cernerConfigured = false;
            boolean secureHealthconfigured = false;
            assertEquals(createNumberOfDomains(), domains.size());            
            cernerConfigured = false;
            secureHealthconfigured = false;
            for (String domain : domains)
            {
                if (domain.equalsIgnoreCase("cerner.com"))
                    cernerConfigured = true;
                else if (domain.equalsIgnoreCase("securehealthemail.com"))
                    secureHealthconfigured = true; 
            }                
            assertTrue(cernerConfigured);
            assertTrue(secureHealthconfigured);
        } 
	}
	
	@Test
	public void testDefaultConfigurationNoSettings() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     

        	@Override
            protected void addPublicCertificates() throws Exception
            {

    			
    			Certificate cert = new Certificate();
    			cert.setData(loadCertificateData("cacert.der"));
    			cert.setOwner("gm2552@cerner.com");
    			
    			TestUtils.addSafeCertficate(certService, cert);
 
            }
        	
            @Override
            protected void addPrivateCertificates() throws Exception {

            	addCertificatesToConfig("cacert.der", null, "gm2552@cerner.com");
            	addCertificatesToConfig("cacert.der", null, "gm2552@cerner.com"); 
            	

            	addCertificatesToConfig("cacert.der", null, "test@cerner.com");
            	

            	addCertificatesToConfig("cacert.der", null, "cerner.com");
            	addCertificatesToConfig("cacert.der", null, "cerner.com");
            	addCertificatesToConfig("cacert.der", null, "cerner.com");
            }
            
            @SuppressWarnings("unused")
			protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	DefaultNHINDAgent nAgent = ((DefaultNHINDAgent)agent.getAgent());
            	CertificateResolver privResl = nAgent.getPrivateCertResolver();
            	assertNotNull(privResl);
            	Collection<X509Certificate> certs = privResl.getCertificates(new InternetAddress("gm2552@cerner.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());
            	assertTrue(privResl instanceof ConfigServiceRESTCertificateStore);
            	
            	// do it again to test the cache
            	certs = privResl.getCertificates(new InternetAddress("gm2552@cerner.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());

            	// test singleton
            	certs = privResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());
            	
            	// again for cache
            	certs = privResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());
            	
            	
            	// test unknown user so fall back to domain level
            	certs = privResl.getCertificates(new InternetAddress("bogus@cerner.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());
            	
            	// again for cache
            	certs= privResl.getCertificates(new InternetAddress("bogus@cerner.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());     
            	
            	// test for domain only
            	certs = privResl.getCertificates(new InternetAddress("cerner.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());
            	
            	// again for cache
            	certs = privResl.getCertificates(new InternetAddress("cerner.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());      
            	
            	// assert we have the proper ldap resolvers
            	Collection<CertificateResolver> resolvers = nAgent.getPublicCertResolvers();
            	assertNotNull(resolvers);
            	assertEquals(2, resolvers.size());
            	Iterator<CertificateResolver> iter = resolvers.iterator();
            	
 
            	CertificateResolver ldapPublicStore;
            	assertTrue(iter.next() instanceof DNSCertificateStore);
            	assertTrue((ldapPublicStore = iter.next()) instanceof LDAPCertificateStore);
            	
            }
        }.perform();
    }	
	
	
	@Test
	public void testConfigurationPrivateKeyStoreFile() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     

            protected void addSettings() throws Exception
            {
            	settingService.addSetting("PrivateStoreType", "keystore");
            	settingService.addSetting("PrivateStoreFile", "internalKeystore");
            	settingService.addSetting("PrivateStoreFilePass", "h3||0 wor|d");
            	settingService.addSetting("PrivateStorePrivKeyPass", "pKpa$$wd");
            }
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// already in the keystore file
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	DefaultNHINDAgent nAgent = ((DefaultNHINDAgent)agent.getAgent());
            	CertificateResolver privResl = nAgent.getPrivateCertResolver();
            	assertNotNull(privResl);
            	Collection<X509Certificate> certs = privResl.getCertificates(new InternetAddress("ryan@messaging.cernerdemos.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());
            	assertTrue(privResl instanceof KeyStoreCertificateStore);
            	
            }
        }.perform();
    }
	
	@Test
	public void testConfigurationPublicKeyStoreFile() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     

            protected void addSettings() throws Exception
            {
            	settingService.addSetting("PublicStoreType", "keystore");
            	settingService.addSetting("PublicStoreFile", "internalKeystore");
            	settingService.addSetting("PublicStoreFilePass", "h3||0 wor|d");
            	settingService.addSetting("PublicStorePrivKeyPass", "pKpa$$wd");
            }
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// already in the keystore file
            }
            
            @SuppressWarnings("deprecation")
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	DefaultNHINDAgent nAgent = ((DefaultNHINDAgent)agent.getAgent());
            	CertificateResolver pubResl = nAgent.getPublicCertResolver();
            	assertNotNull(pubResl);
            	Collection<X509Certificate> certs = pubResl.getCertificates(new InternetAddress("ryan@messaging.cernerdemos.com"));
            	assertNotNull(certs);
            	assertEquals(1, certs.size());
            	assertTrue(pubResl instanceof KeyStoreCertificateStore);
            	
            }
        }.perform();
    }	
	
	@Test
	public void testConfigurationAnchorKeyStoreFile() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     

            protected void addSettings() throws Exception
            {
            	settingService.addSetting("AnchorStoreType", "keystore");
            	settingService.addSetting("AnchorResolverType", "multidomain");
            	settingService.addSetting("AnchorKeyStoreFile", "internalKeystore");
            	settingService.addSetting("AnchorKeyStoreFilePass", "h3||0 wor|d");
            	settingService.addSetting("AnchorKeyStorePrivKeyPass", "pKpa$$wd");
            	
            	settingService.addSetting("cerner.comIncomingAnchorAliases", "cacert");
            	settingService.addSetting("securehealthemail.comIncomingAnchorAliases", "secureHealthEmailCACert");
            	
            	settingService.addSetting("cerner.comOutgoingAnchorAliases", "cacert");
            	settingService.addSetting("securehealthemail.comOutgoingAnchorAliases", "secureHealthEmailCACert");            	
            }
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// doesn't matter
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	DefaultNHINDAgent nAgent = ((DefaultNHINDAgent)agent.getAgent());

            	
            	
            	CertificateResolver trustResl = nAgent.getTrustAnchors().getIncomingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// get the incoming trust anchor for cerner.com
            	Collection<X509Certificate> anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());
            	
            	// get the incoming trust anchor for securehealthemail.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());       
            	
            	
            	
            	trustResl = nAgent.getTrustAnchors().getOutgoingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// get the outgoing trust anchor for cerner.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());
            	
            	// get the outgoing trust anchor for securehealthemail.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());    
            	
            }
        }.perform();
    }		
	
	@Test
	public void testOutboundOnlyAnchors_keyStoreAnchor() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     

            protected void addSettings() throws Exception
            {
            	settingService.addSetting("AnchorStoreType", "keystore");
            	settingService.addSetting("AnchorResolverType", "multidomain");
            	settingService.addSetting("AnchorKeyStoreFile", "internalKeystore");
            	settingService.addSetting("AnchorKeyStoreFilePass", "h3||0 wor|d");
            	settingService.addSetting("AnchorKeyStorePrivKeyPass", "pKpa$$wd");
            	
            	settingService.addSetting("cerner.comOutgoingAnchorAliases", "cacert");
            	settingService.addSetting("securehealthemail.comOutgoingAnchorAliases", "secureHealthEmailCACert");            	
            }
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// doesn't matter
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	DefaultNHINDAgent nAgent = ((DefaultNHINDAgent)agent.getAgent());

            	
            	
            	CertificateResolver trustResl = nAgent.getTrustAnchors().getIncomingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// assert 0 incoming anchors
            	Collection<X509Certificate> anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(0, anchors.size());
            	
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(0, anchors.size());       
            	
            	
            	
            	trustResl = nAgent.getTrustAnchors().getOutgoingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// get the outgoing trust anchor for cerner.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());
            	
            	// get the outgoing trust anchor for securehealthemail.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());    
            	
            }
        }.perform();
    }
	
	@Test
	public void testOutboundOnlyAnchors_WSStoreAnchor() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// doesn't matter
            }
            
            @Override
            protected void addTrustAnchors() throws Exception
            {
            	
            	Anchor anchor = new Anchor();
            	anchor.setCertificateData(getCertificateFileData("cacert.der"));
            	anchor.setOwner("cerner.com");
            	anchor.setIncoming(false);
            	anchor.setOutgoing(true);
            	anchor.setStatus(EntityStatus.ENABLED);
            	anchorService.addAnchor(anchor);
            	
            	anchor = new Anchor();
            	anchor.setCertificateData(getCertificateFileData("cacert.der"));
            	anchor.setOwner("securehealthemail.com");
            	anchor.setIncoming(false);
            	anchor.setOutgoing(true);
            	anchor.setStatus(EntityStatus.ENABLED);
            	anchorService.addAnchor(anchor);
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	DefaultNHINDAgent nAgent = ((DefaultNHINDAgent)agent.getAgent());

            	CertificateResolver trustResl = nAgent.getTrustAnchors().getIncomingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// assert 0 incoming anchors
            	Collection<X509Certificate> anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(0, anchors.size());
            	
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(0, anchors.size());       
            	
            	
            	
            	trustResl = nAgent.getTrustAnchors().getOutgoingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// get the outgoing trust anchor for cerner.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());
            	
            	// get the outgoing trust anchor for securehealthemail.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());    
            	
            }
        }.perform();
    }

	public void testInboundOnlyAnchors_WSTrustBundleAnchors_SingleBundle() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	protected Collection<Domain> domainsTested;
        	protected Collection<TrustBundle> bundlesTested;
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// doesn't matter
            }
            
            @Override
            protected void addTrustAnchors() throws Exception
            {
            	final File bundleFile = new File("src/test/resources/bundles/testBundle.p7b");
            	
            	final TrustBundle bundle = new TrustBundle();
            	bundle.setBundleName("TestBundle");
            	bundle.setBundleURL(filePrefix + bundleFile.getAbsolutePath());
            	bundle.setRefreshInterval(0);
            	
            	
            	bundleService.addTrustBundle(bundle);
            	
            	// load the bundles
            	Thread.sleep(2000);
            	
            	bundlesTested = bundleService.getTrustBundles(true);
            	
    			
    			domainsTested = domainService.searchDomains("", null);
    			
    			for (Domain domain : domainsTested)
    				for (TrustBundle testBundle : bundlesTested)
    					bundleService.associateTrustBundleToDomain(domain.getDomainName(), testBundle.getBundleName(), true, false);
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	DefaultNHINDAgent nAgent = ((DefaultNHINDAgent)agent.getAgent());

            	CertificateResolver trustResl = nAgent.getTrustAnchors().getOutgoingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// assert 0 incoming anchors
            	Collection<X509Certificate> anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(0, anchors.size());
            	
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(0, anchors.size());       
            	
            	
            	
            	trustResl = nAgent.getTrustAnchors().getIncomingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// get the outgoing trust anchor for cerner.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());
            	
            	// get the outgoing trust anchor for securehealthemail.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());    
            	
            }
        }.perform();
    }
	
	public void testOutboundOnlyAnchors_WSTrustBundleAnchors_SingleBundle() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	protected Collection<Domain> domainsTested;
        	protected Collection<TrustBundle> bundlesTested;
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// doesn't matter
            }
            
            @Override
            protected void addTrustAnchors() throws Exception
            {
            	final File bundleFile = new File("src/test/resources/bundles/testBundle.p7b");
            	
            	final TrustBundle bundle = new TrustBundle();
            	bundle.setBundleName("TestBundle");
            	bundle.setBundleURL(filePrefix + bundleFile.getAbsolutePath());
            	bundle.setRefreshInterval(0);
            	
            	
            	bundleService.addTrustBundle(bundle);
            	
            	// load the bundles
            	Thread.sleep(2000);
            	
            	bundlesTested = bundleService.getTrustBundles(true);
            	
    			
    			domainsTested = domainService.searchDomains("", null);
    			
    			for (Domain domain : domainsTested)
    				for (TrustBundle testBundle : bundlesTested)
    					bundleService.associateTrustBundleToDomain(domain.getDomainName(), testBundle.getBundleName(), false, true);
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	DefaultNHINDAgent nAgent = ((DefaultNHINDAgent)agent.getAgent());

            	CertificateResolver trustResl = nAgent.getTrustAnchors().getIncomingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// assert 0 incoming anchors
            	Collection<X509Certificate> anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(0, anchors.size());
            	
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(0, anchors.size());       
            	
            	
            	
            	trustResl = nAgent.getTrustAnchors().getOutgoingAnchors();
            	assertTrue(trustResl instanceof TrustAnchorCertificateStore);
            	
            	// get the outgoing trust anchor for cerner.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@cerner.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());
            	
            	// get the outgoing trust anchor for securehealthemail.com
            	anchors = trustResl.getCertificates(new InternetAddress("test@securehealthemail.com"));
            	assertNotNull(anchors);
            	assertEquals(1, anchors.size());    
            	
            }
        }.perform();
    }

	public void testPublicPolicy_assertPolicyDomainAndDirection() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	protected Domain domainTested;
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// doesn't matter
            }
            
            @Override
            protected void addDomains() throws Exception
            {
            	super.addDomains();
            	
            	// add some policies
            	Collection<Domain> domains = domainService.searchDomains("", null);
            	domainTested = domains.iterator().next();
            	
            	CertPolicy policy = new CertPolicy();
            	policy.setPolicyName("Test Incoming Policy");
            	policy.setLexicon(PolicyLexicon.XML);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("dataEnciphermentOnlyRequired.xml"));
            	certPolService.addPolicy(policy);
            	
            	policy = new CertPolicy();
            	policy.setPolicyName("Test Outgoing Policy");
            	policy.setLexicon(PolicyLexicon.XML);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("dataEnciphermentOnlyRequired.xml"));
            	certPolService.addPolicy(policy);
            	
            	CertPolicyGroup group = new CertPolicyGroup();
            	group.setPolicyGroupName("Test Policy Group");
            	certPolService.addPolicyGroup(group);
            	
            	group = certPolService.getPolicyGroup("Test Policy Group");
            	policy = certPolService.getPolicyByName("Test Incoming Policy");
            	
            	CertPolicyGroupUse use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PUBLIC_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(policy);
            	
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            
            	policy = certPolService.getPolicyByName("Test Outgoing Policy");
            	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PUBLIC_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(policy);
            	
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            	
            	certPolService.associatePolicyGroupToDomain(group.getPolicyGroupName(), domainTested.getDomainName());
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
 
            	final MutableAgent mutableNHINDAgent = (MutableAgent)agent.getAgent();
            	final PolicyResolver publicResolver = mutableNHINDAgent.getPublicPolicyResolver();
            	
            	Collection<PolicyExpression> expressions = 
            			publicResolver.getOutgoingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(1, expressions.size());
            	
            	expressions = publicResolver.getIncomingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(1, expressions.size());
            	
            	expressions =  publicResolver.getOutgoingPolicy(new InternetAddress("me@notthere.com"));
            	assertEquals(0, expressions.size());
            	
            	final PolicyResolver privateResolver = mutableNHINDAgent.getPrivatePolicyResolver();
            	expressions = 
            			privateResolver.getOutgoingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(0, expressions.size());
            	
            	expressions = privateResolver.getIncomingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(0, expressions.size());
            }
        }.perform();
    }
	
	public void testPrivatePolicy_assertPolicyDomainAndDirection() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	protected Domain domainTested;
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// doesn't matter
            }
            
            @Override
            protected void addDomains() throws Exception
            {
            	super.addDomains();
            	
            	// add some policies
            	Collection<Domain> domains = domainService.searchDomains("", null);
            	domainTested = domains.iterator().next();
            	
            	CertPolicy policy = new CertPolicy();
            	policy.setPolicyName("Test Incoming Policy");
            	policy.setLexicon(PolicyLexicon.XML);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("dataEnciphermentOnlyRequired.xml"));
            	certPolService.addPolicy(policy);
            	
            	policy = new CertPolicy();
            	policy.setPolicyName("Test Outgoing Policy");
            	policy.setLexicon(PolicyLexicon.XML);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("dataEnciphermentOnlyRequired.xml"));
            	certPolService.addPolicy(policy);
            	
            	CertPolicyGroup group = new CertPolicyGroup();
            	group.setPolicyGroupName("Test Policy Group");
            	certPolService.addPolicyGroup(group);
            	
            	group = certPolService.getPolicyGroup("Test Policy Group");
            	policy = certPolService.getPolicyByName("Test Incoming Policy");
            	
            	CertPolicyGroupUse use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PRIVATE_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(policy);            	
            	
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            
            	policy = certPolService.getPolicyByName("Test Outgoing Policy");
            	
            	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PRIVATE_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(policy);             	
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            	
            	certPolService.associatePolicyGroupToDomain(group.getPolicyGroupName(), domainTested.getDomainName());
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
 
            	final MutableAgent mutableNHINDAgent = (MutableAgent)agent.getAgent();
            	final PolicyResolver privateResolver = mutableNHINDAgent.getPrivatePolicyResolver();
            	
            	Collection<PolicyExpression> expressions = 
            			privateResolver.getOutgoingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(1, expressions.size());
            	
            	expressions = privateResolver.getIncomingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(1, expressions.size());
            	
            	expressions =  privateResolver.getOutgoingPolicy(new InternetAddress("me@notthere.com"));
            	assertEquals(0, expressions.size());
            	
            	final PolicyResolver publicResolver = mutableNHINDAgent.getPublicPolicyResolver();
            	expressions = 
            			publicResolver.getOutgoingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(0, expressions.size());
            	
            	expressions = publicResolver.getIncomingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(0, expressions.size());
            }
        }.perform();
    }
	
	public void testTrustPolicy_assertPolicyDomainAndDirection() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	protected Domain domainTested;
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {
            	// doesn't matter
            }
            
            @Override
            protected void addDomains() throws Exception
            {
            	super.addDomains();
            	
            	// add some policies
            	Collection<Domain> domains = domainService.searchDomains("", null);
            	domainTested = domains.iterator().next();
            	
            	CertPolicy policy = new CertPolicy();
            	policy.setPolicyName("Test Incoming Policy");
            	policy.setLexicon(PolicyLexicon.XML);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("dataEnciphermentOnlyRequired.xml"));
            	certPolService.addPolicy(policy);
            	
            	policy = new CertPolicy();
            	policy.setPolicyName("Test Outgoing Policy");
            	policy.setLexicon(PolicyLexicon.XML);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("dataEnciphermentOnlyRequired.xml"));
            	certPolService.addPolicy(policy);
            	
            	CertPolicyGroup group = new CertPolicyGroup();
            	group.setPolicyGroupName("Test Policy Group");
            	certPolService.addPolicyGroup(group);
            	
            	group = certPolService.getPolicyGroup("Test Policy Group");
            	policy = certPolService.getPolicyByName("Test Incoming Policy");
            	
            	CertPolicyGroupUse use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.TRUST);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(policy);             	
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            
            	policy = certPolService.getPolicyByName("Test Outgoing Policy");
            	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.TRUST);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(policy);            	
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            	
            	certPolService.associatePolicyGroupToDomain(group.getPolicyGroupName(), domainTested.getDomainName());
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
 
            	final MutableAgent mutableNHINDAgent = (MutableAgent)agent.getAgent();
            	
            	final PolicyResolver trustResolver = mutableNHINDAgent.getTrustModel().getTrustPolicyResolver();
            	
            	// trust policies are the same for incoming and outgoing
                Collection<PolicyExpression> expressions = 
                		trustResolver.getOutgoingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(2, expressions.size());
            	
            	// trust policies are the same for incoming and outgoing
            	expressions = trustResolver.getIncomingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(2, expressions.size());
            	
            	expressions =  trustResolver.getOutgoingPolicy(new InternetAddress("me@notthere.com"));
            	assertEquals(0, expressions.size());
            			
            	final PolicyResolver privateResolver = mutableNHINDAgent.getPrivatePolicyResolver();
            	
            	expressions = privateResolver.getOutgoingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(0, expressions.size());
            	
            	expressions = privateResolver.getIncomingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(0, expressions.size());
            	
            	
            	final PolicyResolver publicResolver = mutableNHINDAgent.getPublicPolicyResolver();
            	expressions = 
            			publicResolver.getOutgoingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(0, expressions.size());
            	
            	expressions = publicResolver.getIncomingPolicy(new InternetAddress("me@" + domainTested.getDomainName()));
            	assertEquals(0, expressions.size());
            }
        }.perform();
    }
	
	public void testCertResolutionWithPolicy_outgoing_dualUseCertificates() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	
        	@Override
        	protected void addSettings() throws Exception
        	{
        		super.addSettings();
        		
        		settingService.addSetting("PublicStoreType", "WS");
        	}
        	
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {

            	addCertificatesToConfig("digSigOnly.der", "digSigOnlyKey.der", "externUser1@starugh-stateline.com");
            	addCertificatesToConfig("dualUse.der", "dualUseKey.der", "externUser1@starugh-stateline.com");
            	addCertificatesToConfig("keyEncOnly.der", "keyEncOnlyKey.der", "externUser1@starugh-stateline.com");
            	
            	addCertificatesToConfig("digSigOnly.der", "digSigOnlyKey.der", "User1@Cerner.com");
            	addCertificatesToConfig("dualUse.der", "dualUseKey.der", "User1@Cerner.com");
            	addCertificatesToConfig("keyEncOnly.der", "keyEncOnlyKey.der", "User1@Cerner.com");
            }
            
            @Override
            protected void addDomains() throws Exception
            {
            	Domain dom = new Domain();
            	dom.setDomainName("cerner.com");

            	domainService.addDomain(dom);
            	
            	// add some policies
            	Collection<Domain> domains = domainService.searchDomains("", null); 
            	
            	CertPolicy policy = new CertPolicy();
            	policy.setPolicyName("DualUse");
            	policy.setLexicon(PolicyLexicon.SIMPLE_TEXT_V1);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("dualUseCertRequired.txt"));
            	certPolService.addPolicy(policy);
            
            	
            	CertPolicyGroup group = new CertPolicyGroup();
            	group.setPolicyGroupName("Test Policy Group");
            	certPolService.addPolicyGroup(group);
            	
            	group = certPolService.getPolicyGroup("Test Policy Group");
            	policy = certPolService.getPolicyByName("DualUse");
            	
            	CertPolicyGroupUse use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PUBLIC_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(true);
            	use.setPolicy(policy);     	
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            
            	
               	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PRIVATE_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(true);
            	use.setPolicy(policy); 
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            	
            	certPolService.associatePolicyGroupToDomain(group.getPolicyGroupName(), domains.iterator().next().getDomainName());
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	
            	final X509Certificate policyFilteredCert = TestUtils.loadCertificate("dualUse.der", null);
            	
            	final MimeMessage msg = new MimeMessage(null, FileUtils.openInputStream(new File("./src/test/resources/messages/PlainOutgoingMessage.txt")));
            	final OutgoingMessage outgoingMessage = new OutgoingMessage(new Message(msg));
            	outgoingMessage.setAgent(agent.getAgent());
            	
            	NHINDAgentAccessor.bindAddresses(agent.getAgent(), outgoingMessage);
            	
            	final NHINDAddressCollection recips = outgoingMessage.getRecipients();
            	assertEquals(1, recips.getCertificates().size());
            	assertEquals(policyFilteredCert, recips.getCertificates().iterator().next());
            	
            	final NHINDAddress sender = outgoingMessage.getSender();           	
            	assertEquals(1, sender.getCertificates().size());
            	assertEquals(policyFilteredCert, sender.getCertificates().iterator().next());
            }
        }.perform();
    }
	
	public void testCertResolutionWithPolicy_incoming_dualUseCertificates() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	
        	@Override
        	protected void addSettings() throws Exception
        	{
        		super.addSettings();
        		
        		settingService.addSetting("PublicStoreType", "WS");
        	}
        	
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {

            	addCertificatesToConfig("digSigOnly.der", "digSigOnlyKey.der", "externUser1@starugh-stateline.com");
            	addCertificatesToConfig("dualUse.der", "dualUseKey.der", "externUser1@starugh-stateline.com");
            	addCertificatesToConfig("keyEncOnly.der", "keyEncOnlyKey.der", "externUser1@starugh-stateline.com");
            	
            	addCertificatesToConfig("digSigOnly.der", "digSigOnlyKey.der", "User1@Cerner.com");
            	addCertificatesToConfig("dualUse.der", "dualUseKey.der", "User1@Cerner.com");
            	addCertificatesToConfig("keyEncOnly.der", "keyEncOnlyKey.der", "User1@Cerner.com");
            }
            
            @Override
            protected void addDomains() throws Exception
            {
            	Domain dom = new Domain();
            	dom.setDomainName("cerner.com");
            	domainService.addDomain(dom);
            	
            	// add some policies
            	Collection<Domain> domains = domainService.searchDomains("", null);
            	
            	CertPolicy policy = new CertPolicy();
            	policy.setPolicyName("DualUse");
            	policy.setLexicon(PolicyLexicon.SIMPLE_TEXT_V1);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("dualUseCertRequired.txt"));
            	certPolService.addPolicy(policy);
            
            	
            	CertPolicyGroup group = new CertPolicyGroup();
            	group.setPolicyGroupName("Test Policy Group");
            	certPolService.addPolicyGroup(group);
            	
            	group = certPolService.getPolicyGroup("Test Policy Group");
            	policy = certPolService.getPolicyByName("DualUse");
            	
            	CertPolicyGroupUse use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PUBLIC_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(true);
            	use.setPolicy(policy); 
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            
            	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PRIVATE_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(true);
            	use.setPolicy(policy); 
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            	
            	certPolService.associatePolicyGroupToDomain(group.getPolicyGroupName(), domains.iterator().next().getDomainName());
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	
            	final X509Certificate policyFilteredCert = TestUtils.loadCertificate("dualUse.der", null);
            	
            	final MimeMessage msg = new MimeMessage(null, FileUtils.openInputStream(new File("./src/test/resources/messages/PlainIncomingMessage.txt")));
            	final IncomingMessage incomingMessage = new IncomingMessage(new Message(msg));
            	incomingMessage.setAgent(agent.getAgent());
            	
            	NHINDAgentAccessor.bindAddresses(agent.getAgent(), incomingMessage);
            	
            	final NHINDAddressCollection recips = incomingMessage.getRecipients();
            	assertEquals(1, recips.getCertificates().size());
            	assertEquals(policyFilteredCert, recips.getCertificates().iterator().next());

            }
        }.perform();
    }
	
	public void testCertResolutionWithPolicy_outgoing_singleUseOnlyCertificates() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	
        	@Override
        	protected void addSettings() throws Exception
        	{
        		super.addSettings();
        		
        		settingService.addSetting("PublicStoreType", "WS");
        	}
        	
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {

            	addCertificatesToConfig("digSigOnly.der", "digSigOnlyKey.der", "externUser1@starugh-stateline.com");
            	addCertificatesToConfig("dualUse.der", "dualUseKey.der", "externUser1@starugh-stateline.com");
            	addCertificatesToConfig("keyEncOnly.der", "keyEncOnlyKey.der", "externUser1@starugh-stateline.com");
            	
            	addCertificatesToConfig("digSigOnly.der", "digSigOnlyKey.der", "User1@Cerner.com");
            	addCertificatesToConfig("dualUse.der", "dualUseKey.der", "User1@Cerner.com");
            	addCertificatesToConfig("keyEncOnly.der", "keyEncOnlyKey.der", "User1@Cerner.com");
            }
            
            @Override
            protected void addDomains() throws Exception
            {
            	Domain dom = new Domain();
            	dom.setDomainName("cerner.com");
            	domainService.addDomain(dom);
            	
            	// add some policies
            	Collection<Domain> domains = domainService.searchDomains("", null);
            	
            	CertPolicy policy = new CertPolicy();
            	policy.setPolicyName("KeyEncOnly");
            	policy.setLexicon(PolicyLexicon.SIMPLE_TEXT_V1);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("keyEncOnly.txt"));
            	certPolService.addPolicy(policy);
            
            	policy = new CertPolicy();
            	policy.setPolicyName("DigiSigOnly");
            	policy.setLexicon(PolicyLexicon.SIMPLE_TEXT_V1);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("digiSigOnly.txt"));
            	certPolService.addPolicy(policy);
            	
            	CertPolicy dataEncPolicy = certPolService.getPolicyByName("KeyEncOnly");
            	CertPolicy digSigPolicy = certPolService.getPolicyByName("DigiSigOnly");
            	
            	CertPolicyGroup group = new CertPolicyGroup();
            	group.setPolicyGroupName("Test Policy Group");
            	certPolService.addPolicyGroup(group);
            	
            	group = certPolService.getPolicyGroup("Test Policy Group");
            	// for outgoing messages, we want the public resolver to only get data encipherment and the private resolver to digital signatures
            	
            	CertPolicyGroupUse use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PUBLIC_RESOLVER);
            	use.setIncoming(false);
            	use.setOutgoing(true);
            	use.setPolicy(dataEncPolicy); 
            	
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            	
            	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PUBLIC_RESOLVER);
            	use.setIncoming(false);
            	use.setOutgoing(true);
            	use.setPolicy(digSigPolicy); 
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);            	

            	// for incoming messages, we want the private resolver to only get data encipherment and the public resolver to digital signatures
            	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PRIVATE_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(digSigPolicy); 
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(),use);
            	
               	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PRIVATE_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(dataEncPolicy); 
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);  
            	
            	certPolService.associatePolicyGroupToDomain(group.getPolicyGroupName(), domains.iterator().next().getDomainName());
            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	
            	final X509Certificate digSigCert = TestUtils.loadCertificate("digSigOnly.der", null);
            	final X509Certificate keyEncCert = TestUtils.loadCertificate("keyEncOnly.der", null);
            	
            	final MimeMessage msg = new MimeMessage(null, FileUtils.openInputStream(new File("./src/test/resources/messages/PlainOutgoingMessage.txt")));
            	final OutgoingMessage outgoingMessage = new OutgoingMessage(new Message(msg));
            	outgoingMessage.setAgent(agent.getAgent());
            	
            	NHINDAgentAccessor.bindAddresses(agent.getAgent(), outgoingMessage);
            	
            	final NHINDAddressCollection recips = outgoingMessage.getRecipients();
            	assertEquals(1, recips.getCertificates().size());
            	assertEquals(keyEncCert, recips.getCertificates().iterator().next());
            	
            	final NHINDAddress sender = outgoingMessage.getSender();           	
            	assertEquals(1, sender.getCertificates().size());
            	assertEquals(digSigCert, sender.getCertificates().iterator().next());
            }
        }.perform();
    }
	
	public void testCertResolutionWithPolicy_incoming_singleUseOnlyCertificates() throws Exception 
    {
        new MultiDomainTestPlan() 
        {                     
        	
        	@Override
        	protected void addSettings() throws Exception
        	{
        		super.addSettings();
        		
        		settingService.addSetting("PublicStoreType", "WS");
        	}
        	
        	
            @Override
            protected void addPrivateCertificates() throws Exception
            {

            	addCertificatesToConfig("digSigOnly.der", "digSigOnlyKey.der", "externUser1@starugh-stateline.com");
            	addCertificatesToConfig("dualUse.der", "dualUseKey.der", "externUser1@starugh-stateline.com");
            	addCertificatesToConfig("keyEncOnly.der", "keyEncOnlyKey.der", "externUser1@starugh-stateline.com");
            	
            	addCertificatesToConfig("digSigOnly.der", "digSigOnlyKey.der", "User1@Cerner.com");
            	addCertificatesToConfig("dualUse.der", "dualUseKey.der", "User1@Cerner.com");
            	addCertificatesToConfig("keyEncOnly.der", "keyEncOnlyKey.der", "User1@Cerner.com");
            }
            
            @Override
            protected void addDomains() throws Exception
            {
            	Domain dom = new Domain();
            	dom.setDomainName("cerner.com");
            	domainService.addDomain(dom);
            	
            	// add some policies
            	Collection<Domain> domains = domainService.searchDomains("", null);
            	
            	CertPolicy policy = new CertPolicy();
            	policy.setPolicyName("KeyEncOnly");
            	policy.setLexicon(PolicyLexicon.SIMPLE_TEXT_V1);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("keyEncOnly.txt"));
            	certPolService.addPolicy(policy);
            
            	policy = new CertPolicy();
            	policy.setPolicyName("DigiSigOnly");
            	policy.setLexicon(PolicyLexicon.SIMPLE_TEXT_V1);
            	policy.setPolicyData(TestUtils.readBytePolicyResource("digiSigOnly.txt"));
            	certPolService.addPolicy(policy);
            	
            	CertPolicy dataEncPolicy = certPolService.getPolicyByName("KeyEncOnly");
            	CertPolicy digSigPolicy = certPolService.getPolicyByName("DigiSigOnly");
            	
            	CertPolicyGroup group = new CertPolicyGroup();
            	group.setPolicyGroupName("Test Policy Group");
            	certPolService.addPolicyGroup(group);
            	
            	group = certPolService.getPolicyGroup("Test Policy Group");

            	CertPolicyGroupUse use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PUBLIC_RESOLVER);
            	use.setIncoming(false);
            	use.setOutgoing(true);
            	use.setPolicy(dataEncPolicy); 
            	
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);
            	
            	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PUBLIC_RESOLVER);
            	use.setIncoming(false);
            	use.setOutgoing(true);
            	use.setPolicy(digSigPolicy); 
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);            	

            	// for incoming messages, we want the private resolver to only get data encipherment and the public resolver to digital signatures
            	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PRIVATE_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(digSigPolicy); 
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(),use);
            	
               	use = new CertPolicyGroupUse();
            	use.setPolicyUse(CertPolicyUse.PRIVATE_RESOLVER);
            	use.setIncoming(true);
            	use.setOutgoing(false);
            	use.setPolicy(dataEncPolicy); 
            	certPolService.addPolicyUseToGroup(group.getPolicyGroupName(), use);  
            	
            	certPolService.associatePolicyGroupToDomain(group.getPolicyGroupName(), domains.iterator().next().getDomainName());

            }
            
            protected void doAssertions(SmtpAgent agent) throws Exception
            {
            	super.doAssertions(agent);
            	
            	final X509Certificate keyEncCert = TestUtils.loadCertificate("keyEncOnly.der", null);
            	
            	final MimeMessage msg = new MimeMessage(null, FileUtils.openInputStream(new File("./src/test/resources/messages/PlainIncomingMessage.txt")));
            	final IncomingMessage incomingMessage = new IncomingMessage(new Message(msg));
            	incomingMessage.setAgent(agent.getAgent());
            	
            	NHINDAgentAccessor.bindAddresses(agent.getAgent(), incomingMessage);
            	
            	final NHINDAddressCollection recips = incomingMessage.getRecipients();
            	assertEquals(1, recips.getCertificates().size());
            	assertEquals(keyEncCert, recips.getCertificates().iterator().next());
            	
            }
        }.perform();
    }
    
	protected byte[] getCertificateFileData(String file) throws Exception
	{
		File fl = new File("src/test/resources/certs/" + file);
		
		return FileUtils.readFileToByteArray(fl);
	}
	
	
    
    protected void addCertificatesToConfig(String certFilename, String keyFileName, String email) throws Exception
    {
    	byte[] dataToAdd = null;
    	if (keyFileName == null)
    	{
    		// just load the cert
    		dataToAdd = loadCertificateData(certFilename);
    	}
    	else
    	{
    		dataToAdd = loadPkcs12FromCertAndKey(certFilename, keyFileName);
    	}
    	
    	Certificate cert = new Certificate();
    	cert.setData(dataToAdd);
    	cert.setOwner(email);
    	
    	TestUtils.addSafeCertficate(certService, cert);
    }
    
	private static byte[] loadPkcs12FromCertAndKey(String certFileName, String keyFileName) throws Exception
	{
		byte[] retVal = null;
		try
		{
			KeyStore localKeyStore = KeyStore.getInstance("PKCS12", CryptoExtensions.getJCEProviderName());
			
			localKeyStore.load(null, null);
			
			byte[] certData = loadCertificateData(certFileName);
			byte[] keyData = loadCertificateData(keyFileName);
			
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			InputStream inStr = new ByteArrayInputStream(certData);
			java.security.cert.Certificate cert = cf.generateCertificate(inStr);
			inStr.close();
			
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec ( keyData );
			Key privKey = kf.generatePrivate (keysp);
			
			char[] array = "".toCharArray();
			
			localKeyStore.setKeyEntry("privCert", privKey, array,  new java.security.cert.Certificate[] {cert});
			
			ByteArrayOutputStream outStr = new ByteArrayOutputStream();
			localKeyStore.store(outStr, array);
			
			retVal = outStr.toByteArray();
			
			outStr.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return retVal;
	}    

	private static byte[] loadCertificateData(String certFileName) throws Exception
	{
		File fl = new File(certBasePath + certFileName);
		
		return FileUtils.readFileToByteArray(fl);
	}
}

