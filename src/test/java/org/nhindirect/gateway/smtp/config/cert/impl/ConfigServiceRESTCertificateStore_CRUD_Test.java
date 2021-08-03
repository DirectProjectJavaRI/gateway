package org.nhindirect.gateway.smtp.config.cert.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.mail.internet.InternetAddress;

import org.apache.jcs.JCS;

import org.nhind.config.rest.CertificateService;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.NHINDException;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.cert.CertStoreCachePolicy;
import org.nhindirect.stagent.cert.CertificateStore;

public class ConfigServiceRESTCertificateStore_CRUD_Test extends SpringBaseTest 
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
				
				addPublicCertificates();
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
            removeFile(keyStoreFile);
            
        }           
        
        protected void removeFile(String filename)
        {
            File delete = new File(filename);
            if (delete.exists())
            	assertTrue(delete.delete());
        }   
        
        protected void addPublicCertificates() throws Exception
        {
 
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
	public void testGetCertificates_EmptyStore_AssertNoRecords() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
				store.flush(true);
				
				assertEquals(0, store.getAllCertificates().size());
			}
		}.perform();
	}	
	
	@Test
	public void testGetCertificates_PopulatedStore_AssertFoundRecords() throws Exception 
	{
		new TestPlan() 
		{	
			
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);
	        	
	        	cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cacert.der", "").getEncoded());
	        	cert.setOwner("test2@example2.com");	        	
	        	certService.addCertificate(cert);
	        	        	
	        }    
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
				store.flush(true);
				
				Collection<X509Certificate> foundCerts = store.getAllCertificates();
				
				assertEquals(2, foundCerts.size());
			}
		}.perform();
	}		
	
	@Test
	public void testGetCertificates_NullProxy_AssertException() throws Exception 
	{
		new TestPlan() 
		{				  			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(null);
			
				boolean exceptionOccured = false;
				try
				{
					store.getAllCertificates();
				}
				catch (NHINDException e)
				{
					exceptionOccured = true;
				}
				
				assertTrue(exceptionOccured);
			}
		}.perform();
	}	
	
	@Test
	public void testGetCertificateByEmail_EmptyStore_AssertNoRecords() throws Exception 
	{
		new TestPlan() 
		{							
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
				store.flush(true);
				
				Collection<X509Certificate> foundCerts = store.getCertificates(new InternetAddress("test1@example.com"));
				
				assertNull(foundCerts);
			}
		}.perform();
	}		
	
	@Test
	public void testGetCertificateByEmail_PopulatedStore_AssertNoRecords() throws Exception 
	{
		new TestPlan() 
		{		
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);	        	        	        	
	        }   			
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
			
				Collection<X509Certificate> foundCerts = store.getCertificates(new InternetAddress("test2@example.com"));
				
				assertNull(foundCerts);
			}
		}.perform();
	}		
	
	@Test
	public void testGetCertificateByEmail_PopulatedStore_AssertSingleRecord() throws Exception 
	{
		new TestPlan() 
		{		
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);	        	        	        	
	        }   			
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
			
				Collection<X509Certificate> foundCerts = store.getCertificates(new InternetAddress("test1@example.com"));
				
				assertEquals(1, foundCerts.size());
			}
		}.perform();
	}		
	
	@Test
	public void testGetCertificateByEmail_PopulatedStore_AssertMultipleRecord() throws Exception 
	{
		new TestPlan() 
		{		
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);	
	        	
	        	cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cacert.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);        	
	        }   			
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
				store.flush(true);
				
				Collection<X509Certificate> foundCerts = store.getCertificates(new InternetAddress("test1@example.com"));
				
				assertEquals(2, foundCerts.size());
			}
		}.perform();
	}		
	
	@Test
	public void testGetCertificateByEmail_PopulatedOrgCertStore_AssertMultipleRecord() throws Exception 
	{
		new TestPlan() 
		{		
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("example.com");	        	
	        	certService.addCertificate(cert);	
	        	
	        	cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cacert.der", "").getEncoded());
	        	cert.setOwner("example.com");	        	
	        	certService.addCertificate(cert);	        	
	        }   			
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
				store.flush(true);
				
				Collection<X509Certificate> foundCerts = store.getCertificates(new InternetAddress("test1@example.com"));
				
				assertEquals(2, foundCerts.size());
			}
		}.perform();
	}		
	
	@Test
	public void testGetCertificateOrgCert_PopulatedOrgCertStore_AssertMultipleRecord() throws Exception 
	{
		new TestPlan() 
		{		
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("example.com");	        	
	        	certService.addCertificate(cert);
	        	
	        	cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cacert.der", "").getEncoded());
	        	cert.setOwner("example.com");	        	
	        	certService.addCertificate(cert);	        	
	        }   			
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
			
				Collection<X509Certificate> foundCerts = store.getCertificates(new InternetAddress("example.com"));
				
				assertEquals(2, foundCerts.size());
			}
		}.perform();
	}		
	
	@Test
	public void testGetCertificateByEmail_NullProxy_AssertException() throws Exception 
	{
		new TestPlan() 
		{				  			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(null);
				store.flush(true);
				
				boolean exceptionOccured = false;
				try
				{
					store.getCertificates(new InternetAddress("test1@example.com"));
				}
				catch (NHINDException e)
				{
					exceptionOccured = true;
				}
				
				assertTrue(exceptionOccured);
			}
		}.perform();
	}		
	
	@Test
	public void testGetCertificateBySubject_EmptyStore_AssertNoRecords() throws Exception 
	{
		new TestPlan() 
		{							
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
				store.flush(true);
				
				Collection<X509Certificate> foundCerts = store.getCertificates("test1@example.com");
				
				assertEquals(0, foundCerts.size());
			}
		}.perform();
	}	
	
	@Test
	public void testGetCertificateBySubject_PopulatedStore_AssertNoRecords() throws Exception 
	{
		new TestPlan() 
		{		
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);        	        	        	
	        }   			
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
			
				Collection<X509Certificate> foundCerts = store.getCertificates("test2@example.com");
				
				assertEquals(0, foundCerts.size());
			}
		}.perform();
	}	
	
	@Test
	public void testGetCertificateBySubject_PopulatedStore_AssertMultipleRecord() throws Exception 
	{
		new TestPlan() 
		{		
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);	
	        	
	        	cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cacert.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);	        	
	        }   			
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
				store.flush(true);
				
				Collection<X509Certificate> foundCerts = store.getCertificates("test1@example.com");
				
				assertEquals(2, foundCerts.size());
			}
		}.perform();
	}		
	
	@Test
	public void testGetCertificateBySubject_PopulatedStore_NullCache_AssertMultipleRecord() throws Exception 
	{
		new TestPlan() 
		{		
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);
	        	
	        	cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cacert.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);        	
	        }   			
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService)
				{
					protected synchronized JCS getCache()
					{						
						return null;
					}
				};
			
				Collection<X509Certificate> foundCerts = store.getCertificates("EMAILADDRESS=test1@example.com");
				
				assertEquals(2, foundCerts.size());
			}
		}.perform();
	}	
	
	@Test
	public void testGetCertificateBySubject_PopulatedStore_NullCache_AssertNoRecord() throws Exception 
	{
		new TestPlan() 
		{		
	        @Override
			protected void addPublicCertificates() throws Exception
	        {
	        	Certificate cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cernerdemos.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);
	        	
	        	cert = new Certificate();
	        	cert.setData(TestUtils.loadCertificate("cacert.der", "").getEncoded());
	        	cert.setOwner("test1@example.com");	        	
	        	certService.addCertificate(cert);	        	
	        }   			
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService)
				{
					protected synchronized JCS getCache()
					{						
						return null;
					}
				};
			
				store.setCachePolicy(null);
				Collection<X509Certificate> foundCerts = store.getCertificates("EMAILADDRESS=test2@example2.com");
				
				assertEquals(0, foundCerts.size());
			}
		}.perform();
	}		
	
	@Test
	public void testContains_AssertException() throws Exception 
	{
		new TestPlan() 
		{				
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
			

				store.update((X509Certificate)null);
			}
		}.perform();
	}		
	
	@Test
	public void testAdd_AssertException() throws Exception 
	{
		new TestPlan() 
		{				
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
			
				boolean execptionOccured = false;
				try				
				{
					store.add((X509Certificate)null);
				}
				catch (UnsupportedOperationException e)
				{
					execptionOccured = true;					
				}
				
				assertTrue(execptionOccured);
			}
		}.perform();
	}	
	
	@Test
	public void testRemove_AssertException() throws Exception 
	{
		new TestPlan() 
		{				
			
			@Override
			protected void performInner() throws Exception
			{
				
				TestConfigServiceCertificateStore store = new TestConfigServiceCertificateStore(certService);
			
				boolean execptionOccured = false;
				try				
				{
					store.remove((X509Certificate)null);
				}
				catch (UnsupportedOperationException e)
				{
					execptionOccured = true;					
				}
				
				assertTrue(execptionOccured);
			}
		}.perform();
	}		
}

