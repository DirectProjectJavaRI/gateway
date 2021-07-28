package org.nhindirect.gateway.smtp.config.cert.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.nhind.config.rest.CertificateService;
import org.nhindirect.common.crypto.PKCS11Credential;
import org.nhindirect.common.crypto.impl.BootstrappedPKCS11Credential;
import org.nhindirect.common.crypto.impl.StaticPKCS11TokenKeyStoreProtectionManager;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.cert.X509CertificateEx;

public class ConfigServiceRESTCertificateStore_getCertificateWithHSMKeyTest
{
	protected CertificateService proxy;
	protected ConfigServiceRESTCertificateStore certService;
	
	@BeforeEach
	public void setUp() throws Exception
	{
		proxy = mock(CertificateService.class);
		
		certService = getCertService();
	}
	
	protected ConfigServiceRESTCertificateStore getCertService() throws Exception
	{
    	if (StringUtils.isEmpty(TestUtils.setupSafeNetToken()))
    		return null;
		
		final ConfigServiceRESTCertificateStore certService = new ConfigServiceRESTCertificateStore(proxy);
		
		final PKCS11Credential cred = new BootstrappedPKCS11Credential("1Kingpuff");
		final StaticPKCS11TokenKeyStoreProtectionManager mgr = 
				new StaticPKCS11TokenKeyStoreProtectionManager(cred, "KeyStoreProtKey", "PrivKeyProtKey");
		
		certService.setKeyStoreProectionManager(mgr);
		
		return certService;
	}
	
	@Test
	public void testGetCertifcateWithPrivKey_noPrivKeyInHSM() throws Exception
	{		
		if (certService == null)
			return;
		
		final X509Certificate cert = TestUtils.loadCertificate("digSigOnly.der", null);
		
		final Certificate modelCert = new Certificate();
		modelCert.setData(cert.getEncoded());

		final Collection<Certificate> certsReturned = new ArrayList<Certificate>();
		certsReturned.add(modelCert);
		
		when(proxy.getCertificatesByOwner((String)any())).thenReturn(certsReturned);
		
		final Collection<X509Certificate> retCerts = certService.getCertificates("test.com");
		assertEquals(1, retCerts.size());
		final X509Certificate retCert = retCerts.iterator().next();
		assertTrue(retCert instanceof X509CertificateEx);
		assertTrue(((X509CertificateEx)retCert).hasPrivateKey());
	}
}
