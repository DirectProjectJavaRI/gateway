package org.nhindirect.gateway.smtp.config.cert.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import java.io.File;

import org.nhind.config.rest.CertificateService;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.cert.CertStoreCachePolicy;
import org.nhindirect.stagent.cert.CertificateStore;
import org.nhindirect.stagent.cert.impl.KeyStoreCertificateStore;

public class ConfigServiceRESTCertificateStore_Construct_Test extends SpringBaseTest
{
	private static final String keyStoreFile = "./target/TempKeyStore";
	
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		@Override
		protected void setupMocks() 
		{
			// create the web service and proxy.... not really mocks
			try
			{				
				removeTestFiles();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
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
        }           
        
        protected void removeFile(String filename)
        {
            File delete = new File(filename);
            delete.delete();
        }   
           
        
		@Override
		protected abstract void performInner() throws Exception;   
		
		protected class TestConfigServiceCertificateStore extends ConfigServiceRESTCertificateStore
		{
			public TestConfigServiceCertificateStore(CertificateService certService)
			{
				super(certService);
			}	

			public TestConfigServiceCertificateStore(CertificateService certService, 
					CertificateStore bootstrapStore, CertStoreCachePolicy policy)
			{
				super(certService, bootstrapStore, policy);
			}	
		}
	}
	
	@Test
	public void testConstructStore_ConfigurationProxyOnly() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
				
				assertNotNull(store.cache);
				
				assertNotNull(store.cachePolicy);
				assertEquals(TestConfigServiceCertificateStore.DEFAULT_WS_MAX_CAHCE_ITEMS, store.cachePolicy.getMaxItems());
				assertEquals(TestConfigServiceCertificateStore.DEFAULT_WS_TTL, store.cachePolicy.getSubjectTTL());
			}
		}.perform();
	}
	
	@Test
	public void testConstructStore_NullBootstrap() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService, null, null);
				
				assertNotNull(store.cache);

				assertNotNull(store.cachePolicy);
				assertEquals(TestConfigServiceCertificateStore.DEFAULT_WS_MAX_CAHCE_ITEMS, store.cachePolicy.getMaxItems());
				assertEquals(TestConfigServiceCertificateStore.DEFAULT_WS_TTL, store.cachePolicy.getSubjectTTL());
			}
		}.perform();
	}	
	
	@Test
	public void testConstructStore_CustomBootstrap() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected void performInner() throws Exception
			{
				KeyStoreCertificateStore keyStore = new KeyStoreCertificateStore(new File(keyStoreFile), "nH!NdK3yStor3", "31visl!v3s");
								
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService, keyStore, null);
				
				assertNotNull(store.cache);
				
				assertNotNull(store.cachePolicy);
				assertEquals(TestConfigServiceCertificateStore.DEFAULT_WS_MAX_CAHCE_ITEMS, store.cachePolicy.getMaxItems());
				assertEquals(TestConfigServiceCertificateStore.DEFAULT_WS_TTL, store.cachePolicy.getSubjectTTL());
			}
		}.perform();
	}	
	
	@Test
	public void testConstructStore_CustomCachePolicy() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected void performInner() throws Exception
			{
				KeyStoreCertificateStore keyStore = new KeyStoreCertificateStore(new File(keyStoreFile), "nH!NdK3yStor3", "31visl!v3s");
								
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService, keyStore, 
						new CertStoreCachePolicy()
				{
					public int getMaxItems() 
					{
						return 50; 
					}

					public int getSubjectTTL() 
					{
						return 3600;
					}
				});
				
				assertNotNull(store.cache);
				
				assertNotNull(store.cachePolicy);
				assertEquals(50, store.cachePolicy.getMaxItems());
				assertEquals(3600, store.cachePolicy.getSubjectTTL());
			}
		}.perform();
	}		
}
