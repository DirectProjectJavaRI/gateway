package org.nhindirect.gateway.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.mail.internet.InternetAddress;

import org.apache.commons.io.FileUtils;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.stagent.MutableAgent;
import org.nhindirect.stagent.SpringBaseTest;
import org.nhindirect.stagent.trust.TrustAnchorResolver;

public class GatewayState_updateAgentSettingsTest extends SpringBaseTest
{
	protected byte[] getCertificateFileData(String file) throws Exception
	{
		File fl = new File("src/test/resources/certs/" + file);
		
		return FileUtils.readFileToByteArray(fl);
	}
	
	abstract class TestPlan extends BaseTestPlan 
    {	
		protected void setupMocks() 
		{
			
			try
			{			
				removeTestFiles();
				
				addConfiguration();
			}
			catch (Throwable t)
			{
				throw new RuntimeException(t);
			}
		}
		
        protected void addConfiguration() throws Exception
        {
        	addDomains();
        	
        	addTrustAnchors();
        	
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
        
	    public void tearDown() throws Exception
	    {
        	GatewayState stateInstance = GatewayState.getInstance();
        	stateInstance.setSettingsUpdateInterval(300);
        	
        	if (stateInstance.isAgentSettingManagerRunning())
        		stateInstance.stopAgentSettingsManager();
        	
	    	removeTestFiles();
	        
	    }
	    
        protected void removeFile(String filename){
            File delete = new File(filename);
            delete.delete();
        }
        
        @Override
        protected void performInner() throws Exception 
        {     
        	GatewayState stateInstance = GatewayState.getInstance();
        	
        	if (stateInstance.isAgentSettingManagerRunning())
        		stateInstance.stopAgentSettingsManager();
                      
        	SmtpAgentFactory agentFactory = createAgentFactory();
            SmtpAgent agent = agentFactory.createSmtpAgent();
            
            stateInstance.setSmptAgentFactory(agentFactory);
            stateInstance.setSmtpAgent(agent);
            stateInstance.setSettingsUpdateInterval(1);  // every one second
            stateInstance.startAgentSettingsManager();
            
            doAssertionsOriginalAgentSettings(agent);

            // change the settings
        	Domain dom = new Domain();
        	dom.setDomainName("cernerdirect.com");
        	domainService.addDomain(dom);
        	

        	Anchor anchor = new Anchor();
        	anchor.setCertificateData(getCertificateFileData("cacert.der"));
        	anchor.setOwner("cernerdirect.com");
        	anchor.setIncoming(true);
        	anchor.setOutgoing(true);
        	anchor.setStatus(EntityStatus.ENABLED);
        	anchorService.addAnchor(anchor);
        	
        	// wait 5 seconds to let the service get updated
        	Thread.sleep(5000);
        	
        	
        	doAssertionsNewAgentSettings(agent);
        	
        }  
        
    	protected SmtpAgentFactory createAgentFactory()
    	{
    		return SmtpAgentFactory.getInstance(certService, bundleService, domainService, anchorService, 
    				settingService, certPolService, null, keyStoreMgr);
    	}
        
        protected abstract void doAssertionsOriginalAgentSettings(SmtpAgent agent) throws Exception;
        
        protected abstract void doAssertionsNewAgentSettings(SmtpAgent agent) throws Exception;
    }
	
	@Test
	public void testNewDomainListSettings() throws Exception 
    {
        new TestPlan() 
        {     
            protected void doAssertionsOriginalAgentSettings(SmtpAgent agent) throws Exception
            {
            	assertEquals(2, agent.getAgent().getDomains().size());
            	
            	MutableAgent mutalbeAgent = (MutableAgent)agent.getAgent();
            	Collection<X509Certificate> certs = 
            			mutalbeAgent.getTrustAnchors().getIncomingAnchors().getCertificates(new InternetAddress("cernerdirect.com"));
            	
            	assertEquals(0, certs.size());
            }
            
            protected void doAssertionsNewAgentSettings(SmtpAgent agent) throws Exception
            {
            	assertEquals(3, agent.getAgent().getDomains().size());
            	
            	MutableAgent mutalbeAgent = (MutableAgent)agent.getAgent();
            	TrustAnchorResolver resolver = mutalbeAgent.getTrustAnchors();
            	Collection<X509Certificate> certs = 
            			resolver.getIncomingAnchors().getCertificates(new InternetAddress("joe@cernerdirect.com"));
            	
            	assertEquals(1, certs.size());
            }
        }.perform();
    }
}
