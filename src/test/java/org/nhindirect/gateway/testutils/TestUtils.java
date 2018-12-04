package org.nhindirect.gateway.testutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;

import org.apache.commons.io.FileUtils;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;
import org.nhindirect.common.crypto.CryptoExtensions;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.Domain;


public class TestUtils 
{
	
	private static final String certBasePath = "src/test/resources/certs/";
	
	private static final String policyBasePath = "src/test/resources/policies/";
	
    /**
     * used for testing with a pkcs11 token
     * @return The Security provider name if the token is loaded successfully... an empty string other wise 
     * @throws Exception
     */
	@SuppressWarnings("restriction")
	public static String setupSafeNetToken() throws Exception
	{	
		final CallbackHandler handler = new CallbackHandler()
		{
			public void	handle(Callback[] callbacks)
			{
				for (Callback callback : callbacks)
				{
					if (callback instanceof PasswordCallback)
					{		
						
						 ((PasswordCallback)callback).setPassword("1Kingpuff".toCharArray());
					
					}
				}
			}
		};
		
		sun.security.pkcs11.SunPKCS11 p = null;
		final String configName = "./src/test/resources/pkcs11Config/pkcs11.cfg";
		try
		{
			p = new sun.security.pkcs11.SunPKCS11(configName);
			Security.addProvider(p);
			p.login(null, handler);

		}
		catch (Exception e)
		{
			return "";
		}

		return p.getName();
	}
	
	public static String getTestConfigFile(String fileName)
	{
		File fl = new File("dummy");
		int idx = fl.getAbsolutePath().lastIndexOf("dummy");
		
		String path = fl.getAbsolutePath().substring(0, idx);
		
		return path + "src/test/resources/configFiles/" + fileName;	

	}	

	 public static byte[] readBytePolicyResource(String _rec) throws Exception
	 {
			final String msgResource = policyBasePath + _rec;
			
			return FileUtils.readFileToByteArray(new File(msgResource));
	 }
	 
	 @SuppressWarnings("deprecation")
	public static String readStringPolicyResource(String _rec) throws Exception
	 {
		
			final String msgResource = policyBasePath + _rec;
		
			return FileUtils.readFileToString(new File(msgResource));
	 }
	 
	public static String readMessageResource(String _rec) throws Exception
	{
		
		int BUF_SIZE = 2048;		
		int count = 0;
	
		String msgResource = "/messages/" + _rec;
	
		InputStream stream = TestUtils.class.getResourceAsStream(msgResource);;
				
		ByteArrayOutputStream ouStream = new ByteArrayOutputStream();
		if (stream != null) 
		{
			byte buf[] = new byte[BUF_SIZE];
			
			while ((count = stream.read(buf)) > -1)
			{
				ouStream.write(buf, 0, count);
			}
			
			try 
			{
				stream.close();
			} 
			catch (IOException ieo) 
			{
				throw ieo;
			}
			catch (Exception e)
			{
				throw e;
			}					
		} 
		else
			throw new IOException("Failed to open resource " + _rec);

		return new String(ouStream.toByteArray());		
	}
	 
	public static X509Certificate loadCertificate(String certFileName, String keyFileName) throws Exception
	{
		
		if (keyFileName == null || keyFileName.isEmpty())
		{
			File fl = new File(certBasePath + certFileName);
			return (X509Certificate)CertificateFactory.getInstance("X509", "BC").generateCertificate(FileUtils.openInputStream(fl));
		}	
		else
		{
			return (X509Certificate)CertificateFactory.getInstance("X509", "BC").generateCertificate(new ByteArrayInputStream(loadPkcs12FromCertAndKey(certFileName, keyFileName)));
		}
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
    
    /*
     * Replicates the configuration file names
     */
    
    public static final String VALID_GATEWAY_CONFIG = "ValidGatewayConfig";
    
    public static final String VALID_GATEWAY_STATELINE_CONFIG = "ValidGatewayStatelineConfig";
    
    
    public static void createGatewayConfig(String configName, SettingService settingService, DomainService domainService) throws Exception
    {
    	switch (configName)
    	{
    		case VALID_GATEWAY_CONFIG:
    		{
    			createValidGatewayConfig(settingService, domainService);
    			break;
    		}
    		case VALID_GATEWAY_STATELINE_CONFIG:
    		{
    			createValidGatewayStatelineConfig(settingService, domainService);
    			break;
    		}
    		default:
    			break;
    	}
    }
    
    protected static void createKeyStoreCertSetting(SettingService settingService) throws Exception
    {

		addSafeSetting(settingService, "PublicStoreType", "keystore");
		addSafeSetting(settingService, "PublicStoreFile", "KeyStore");
		addSafeSetting(settingService, "PublicStoreFilePass", "h3||0 wor|d");
		addSafeSetting(settingService, "PublicStorePrivKeyPass", "pKpa$$wd");
		
		addSafeSetting(settingService, "PrivateStoreType", "keystore");
		addSafeSetting(settingService, "PrivateStoreFile", "KeyStore");
		addSafeSetting(settingService, "PrivateStoreFilePass", "h3||0 wor|d");
		addSafeSetting(settingService, "PrivateStorePrivKeyPass", "pKpa$$wd");
		
		addSafeSetting(settingService, "AnchorStoreType", "keystore");
		addSafeSetting(settingService, "AnchorResolverType", "uniform");
		addSafeSetting(settingService, "AnchorKeyStoreFile", "KeyStore");
		addSafeSetting(settingService, "AnchorKeyStoreFilePass", "h3||0 wor|d");
		addSafeSetting(settingService, "AnchorKeyStorePrivKeyPass", "pKpa$$wd");	

    }
    
    protected static void createValidGatewayConfig(SettingService settingService, DomainService domainService) throws Exception
    {    	
    	createKeyStoreCertSetting(settingService);
    	
    	Domain dom = new Domain();
    	dom.setDomainName("cerner.com");
    	addSafeDomain(domainService,dom);    	

    	dom = new Domain();
    	dom.setDomainName("securehealthemail.com");
    	addSafeDomain(domainService,dom);  
    			
    	addSafeSetting(settingService, "cerner.comIncomingAnchorAliases", "cacert,externCaCert,secureHealthEmailCACert,msanchor,cernerDemosCaCert");
    	addSafeSetting(settingService, "cerner.comOutgoingAnchorAliases", "cacert,externCaCert,secureHealthEmailCACert,msanchor,cernerDemosCaCert");
    	addSafeSetting(settingService, "securehealthemail.comIncomingAnchorAliases", "cacert,externCaCert,secureHealthEmailCACert,msanchor,cernerDemosCaCert");
    	addSafeSetting(settingService, "securehealthemail.comOutgoingAnchorAliases", "cacert,externCaCert,secureHealthEmailCACert,msanchor,cernerDemosCaCert");
    }
    
    
    protected static void createValidGatewayStatelineConfig(SettingService settingService, DomainService domainService) throws Exception
    {    	
    	createKeyStoreCertSetting(settingService);
    	
    	Domain dom = new Domain();
    	dom.setDomainName("starugh-stateline.com");
    	addSafeDomain(domainService,dom);    	
    	
    	addSafeSetting(settingService, "starugh-stateline.comIncomingAnchorAliases", "cacert,externCaCert,secureHealthEmailCACert,msanchor,cernerDemosCaCert");
    	addSafeSetting(settingService, "starugh-stateline.comOutgoingAnchorAliases", "cacert,externCaCert,secureHealthEmailCACert,msanchor,cernerDemosCaCert");
    }    
    
	protected static void addSafeSetting(SettingService settingService, String settingName, String settingValue)
	{
		try
		{
			settingService.addSetting(settingName, settingValue);
		}
		catch (Exception e)
		{

		}
	}
	
	protected static void addSafeDomain(DomainService domainService, Domain domain)
	{
		try
		{
			domainService.addDomain(domain);
		}
		catch (Exception e)
		{

		}
	}
	
	public static void addSafeCertficate(CertificateService certService, Certificate cert)
	{
		try
		{
			certService.addCertificate(cert);
		}
		catch (Exception e)
		{

		}
	}	
}
