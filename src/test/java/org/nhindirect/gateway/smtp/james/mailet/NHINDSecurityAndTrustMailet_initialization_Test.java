package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.mailet.MailetConfig;

import org.nhindirect.common.options.OptionsManager;
import org.nhindirect.common.options.OptionsParameter;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.stagent.SpringBaseTest;

public class NHINDSecurityAndTrustMailet_initialization_Test extends SpringBaseTest
{
	abstract class TestPlan extends BaseTestPlan 
	{				
		
		@Override
		protected void setupMocks() 
		{
			OptionsManager.destroyInstance();
		}
		
		protected void tearDownMocks() 
		{
			OptionsManager.destroyInstance();
		}
				
		protected MailetConfig getMailetConfig() throws Exception
		{
			final Map<String,String> params = new HashMap<String, String>();
			
			return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
		}
		
		@Override
		protected void performInner() throws Exception
		{
			NHINDSecurityAndTrustMailet theMailet = new NHINDSecurityAndTrustMailet();

			MailetConfig config = getMailetConfig();
			
			theMailet.init(config);
			doAssertions(theMailet);
		}

		protected void doAssertions(NHINDSecurityAndTrustMailet agent) throws Exception
		{
		}		
		
	}

	protected byte[] getCertificateFileData(String file) throws Exception
	{
		File fl = new File("src/test/resources/certs/" + file);
		
		return FileUtils.readFileToByteArray(fl);
	}	
	
	@Test
	public void testValidMailetConfiguration_AssertProperWSRESTInitialization() throws Exception 
	{
		new TestPlan() 
		{
				
			
			@Override
			protected MailetConfig getMailetConfig() throws Exception
			{
				addDomains();	
				addTrustAnchors();
				
				Map<String,String> params = new HashMap<String, String>();

				
				return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
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
	        
			
			
			@Override
			protected void doAssertions(NHINDSecurityAndTrustMailet agent) throws Exception
			{
				assertNotNull(agent);
				
			}				
		}.perform();
	}	
	
	@Test
	public void testValidJCEProviderConfiguration_assertJCEStrings() throws Exception 
	{
		new TestPlan() 
		{
			protected MailetConfig getMailetConfig() throws Exception
			{
				addDomains();	
				
				Map<String,String> params = new HashMap<String, String>();

				params.put(SecurityAndTrustMailetOptions.JCE_PROVIDER_NAME, "RegJCEProv");
				params.put(SecurityAndTrustMailetOptions.JCE_SENTITIVE_PROVIDER, "SensitiveJCEProv");
				
				return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
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
			
			@Override
			protected void doAssertions(NHINDSecurityAndTrustMailet agent) throws Exception
			{
				assertNotNull(agent);
				assertEquals("RegJCEProv", OptionsManager.getInstance().getParameter(OptionsParameter.JCE_PROVIDER).getParamValue());
				assertEquals("SensitiveJCEProv", OptionsManager.getInstance().getParameter(OptionsParameter.JCE_SENTITIVE_PROVIDER).getParamValue());				
			}				
		}.perform();
	}
	
	@Test
	public void testRejectRoutingTamperConfiguration_notSet_assertFalse() throws Exception 
	{
		new TestPlan() 
		{
			protected MailetConfig getMailetConfig() throws Exception
			{
				addDomains();
				
				Map<String,String> params = new HashMap<String, String>();
				
				return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
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
			
			@Override
			protected void doAssertions(NHINDSecurityAndTrustMailet agent) throws Exception
			{
				assertNotNull(agent);
				assertFalse(OptionsParameter.getParamValueAsBoolean(
						OptionsManager.getInstance().getParameter(OptionsParameter.REJECT_ON_ROUTING_TAMPER), false));			
			}				
		}.perform();
	}	
	@Test
	public void testRejectRoutingTamperConfiguration_set_assertTrue() throws Exception 
	{
		new TestPlan() 
		{
			protected MailetConfig getMailetConfig() throws Exception
			{
				addDomains();
				
				Map<String,String> params = new HashMap<String, String>();
				
				params.put(SecurityAndTrustMailetOptions.REJECT_ON_ROUTING_TAMPER, "true");
				
				return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
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
			
			@Override
			protected void doAssertions(NHINDSecurityAndTrustMailet agent) throws Exception
			{
				assertNotNull(agent);
				assertTrue(OptionsParameter.getParamValueAsBoolean(
						OptionsManager.getInstance().getParameter(OptionsParameter.REJECT_ON_ROUTING_TAMPER), false));			
			}				
		}.perform();
	}	
}
