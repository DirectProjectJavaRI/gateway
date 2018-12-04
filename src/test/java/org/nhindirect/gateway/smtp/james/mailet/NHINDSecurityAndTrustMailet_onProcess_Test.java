package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;
import org.junit.Test;
import org.nhindirect.common.crypto.CryptoExtensions;
import org.nhindirect.common.tx.TxService;
import org.nhindirect.common.tx.impl.NoOpTxServiceClient;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.gateway.smtp.SmtpAgentException;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.mail.MailStandard;
import org.nhindirect.stagent.mail.notifications.MDNStandard;
import org.nhindirect.stagent.parser.EntitySerializer;

public class NHINDSecurityAndTrustMailet_onProcess_Test extends SpringBaseTest
{
	private static final String certBasePath = "src/test/resources/certs/";
	
	static
	{
		CryptoExtensions.registerJCEProviders();
	}
	
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		protected void setupMocks() 
		{
			// create the web service and proxy.... not really mocks
			try
			{

				addConfiguration();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
        protected void removeFile(String filename){
            File delete = new File(filename);
            delete.delete();
        }
		
        
        

		
		protected Mailet getMailet(String configurationName)  throws Exception
		{
			Mailet retVal = null;
			
			TestUtils.createGatewayConfig(configurationName, settingService, domainService);
			
			Map<String,String> params = new HashMap<String, String>();
			
			
			retVal = new OnProcessMailet() 
			{
				@Override
				protected TxService createTxServices()
				{
					return new NoOpTxServiceClient();
				}
			};
			
			MailetConfig mailetConfig = new MockMailetConfig(params, "OverrideOnProcessMailet");
			
			retVal.init(mailetConfig);
			
			return retVal;
		}
		
		
		protected byte[] loadCertificateData(String certFileName) throws Exception
		{
			File fl = new File(certBasePath + certFileName);
			
			return FileUtils.readFileToByteArray(fl);
		}
			
        protected void addConfiguration() throws Exception
        {
        	addDomains();
        	
        	addTrustAnchors();
        	
        	addPublicCertificates();
        	
        	addPrivateCertificates();  
        	
        	addSettings();
        }
        
        protected void addTrustAnchors() throws Exception
        {
        	
        }
        
        protected void addPublicCertificates() throws Exception
        {
        	// default uses DNS
        }
        
        protected void addPrivateCertificates() throws Exception
        {
        	
        }
        
        protected void addSettings() throws Exception
        {
        	// just use default settings
        }
		
        protected void addDomains() throws Exception
        {

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
	    	
	    	certService.addCertificate(cert); 
	    }
	    
	    protected byte[] loadPkcs12FromCertAndKey(String certFileName, String keyFileName) throws Exception
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
		
		@Override
		protected abstract void performInner() throws Exception;
		
		protected abstract String getMessageToProcess() throws Exception;
		
	}	
	
	@Test
	public void testProcessOutgoing_AssertOnPostProcessAndPreProcessedCalled() throws Exception 
	{
		new TestPlan() 
		{			
			protected String getMessageToProcess() throws Exception
			{
				return TestUtils.readMessageResource("PlainOutgoingMessage.txt");
			}	

			
			protected void performInner() throws Exception
			{

				// encrypt
				String originalMessage = getMessageToProcess();
				
				MimeMessage msg = EntitySerializer.Default.deserialize(originalMessage);
				
				// add an MDN request
				msg.setHeader(MDNStandard.Headers.DispositionNotificationTo, msg.getHeader(MailStandard.Headers.From, ","));
				
				MockMail theMessage = new MockMail(msg);
				
				OnProcessMailet theMailet = (OnProcessMailet)getMailet(TestUtils.VALID_GATEWAY_CONFIG);
				
				theMailet.service(theMessage);
								
				assertEquals(1, theMailet.getOnPostProcessCount());
				assertEquals(1, theMailet.getOnPreProcessCount());
				assertEquals(0, theMailet.getOnRejectedCount());
			}				
					
		}.perform();
	}		
	
	@Test
	public void testProcessUntrustedOutgoing_AssertOnRejectedProcessedCalled() throws Exception 
	{
		new TestPlan() 
		{			
			protected String getMessageToProcess() throws Exception
			{
				return TestUtils.readMessageResource("PlainUntrustedOutgoingMessage.txt");
			}	

			
			protected void performInner() throws Exception
			{

				// encrypt
				String originalMessage = getMessageToProcess();
				
				MimeMessage msg = EntitySerializer.Default.deserialize(originalMessage);
				
				// add an MDN request
				msg.setHeader(MDNStandard.Headers.DispositionNotificationTo, msg.getHeader(MailStandard.Headers.From, ","));
				
				MockMail theMessage = new MockMail(msg);
				
				OnProcessMailet theMailet = (OnProcessMailet)getMailet(TestUtils.VALID_GATEWAY_CONFIG);
				
				boolean exceptionOccured = false;
				try
				{
					theMailet.service(theMessage);
				}
				catch (SmtpAgentException e)
				{
					exceptionOccured = true;
				}
				
				assertFalse(exceptionOccured);
				assertEquals(0, theMailet.getOnPostProcessCount());
				assertEquals(1, theMailet.getOnPreProcessCount());
				assertEquals(1, theMailet.getOnRejectedCount());
			}				
					
		}.perform();
	}	
}
