package org.nhindirect.stagent;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nhind.config.rest.AddressService;
import org.nhind.config.rest.AnchorService;
import org.nhind.config.rest.CertPolicyService;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.rest.DNSService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;
import org.nhind.config.rest.TrustBundleService;
import org.nhindirect.common.crypto.KeyStoreProtectionManager;
import org.nhindirect.config.repository.AddressRepository;
import org.nhindirect.config.repository.AnchorRepository;
import org.nhindirect.config.repository.CertPolicyGroupDomainReltnRepository;
import org.nhindirect.config.repository.CertPolicyGroupRepository;
import org.nhindirect.config.repository.CertPolicyRepository;
import org.nhindirect.config.repository.CertificateRepository;
import org.nhindirect.config.repository.DNSRepository;
import org.nhindirect.config.repository.DomainRepository;
import org.nhindirect.config.repository.SettingRepository;
import org.nhindirect.config.repository.TrustBundleDomainReltnRepository;
import org.nhindirect.config.repository.TrustBundleRepository;
import org.nhindirect.config.resources.AddressResource;
import org.nhindirect.config.resources.AnchorResource;
import org.nhindirect.config.resources.CertPolicyResource;
import org.nhindirect.config.resources.CertificateResource;
import org.nhindirect.config.resources.DNSResource;
import org.nhindirect.config.resources.DomainResource;
import org.nhindirect.config.resources.SettingResource;
import org.nhindirect.config.resources.TrustBundleResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource("classpath:bootstrap.properties")
@DirtiesContext
public abstract class SpringBaseTest
{
	protected String filePrefix;
	
	@Autowired
	protected AddressService addressService;
	
	@Autowired 
	protected AddressResource addressResource;
	
	@Autowired
	protected AnchorService anchorService;
	
	@Autowired 
	protected AnchorResource anchorResource;	
	
	@Autowired
	protected CertificateService certService;
	
	@Autowired 
	protected CertificateResource certResource;		
	
	@Autowired
	protected CertPolicyService certPolService;
	
	@Autowired 
	protected CertPolicyResource certPolResource;		

	@Autowired
	protected DomainService domainService;
	
	@Autowired 
	protected DomainResource domainResource;	
	
	@Autowired
	protected DNSService dnsService;
	
	@Autowired
	protected DNSResource dnsResource;
	
	@Autowired
	protected SettingService settingService;
	
	@Autowired
	protected SettingResource settingResource;
	
	@Autowired
	protected TrustBundleService bundleService;
	
	@Autowired
	protected TrustBundleResource bundleResource;
	
	@Autowired
	protected AddressRepository addressRepo;
	
	@Autowired
	protected TrustBundleRepository bundleRepo;	
	
	@Autowired
	protected TrustBundleDomainReltnRepository bundleDomainRepo;
	
	@Autowired
	protected DomainRepository domainRepo;
	
	@Autowired
	protected AnchorRepository anchorRepo;
	
	@Autowired
	protected CertificateRepository certRepo;
	
	@Autowired 
	protected DNSRepository dnsRepo;
	
	@Autowired
	protected SettingRepository settingRepo;
	
	@Autowired
	protected CertPolicyRepository policyRepo;
	
	@Autowired
	protected CertPolicyGroupRepository policyGroupRepo;
	
	@Autowired
	protected CertPolicyGroupDomainReltnRepository groupReltnRepo;
	
	@Autowired
	protected KeyStoreProtectionManager keyStoreMgr;
	
	
	@Before
	public void setUp() throws Exception
	{
		//Thread.sleep(1000);	
		try
		{
			cleanDataStore();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		// clean up the file system
		File dir = new File("./target/tempFiles");
		if (dir.exists())
		try
		{
			FileUtils.cleanDirectory(dir);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected void cleanDataStore() throws Exception
	{		
		anchorRepo.deleteAll();
		
		groupReltnRepo.deleteAll();
		
		addressRepo.deleteAll();
		
		bundleDomainRepo.deleteAll();
		
		bundleRepo.deleteAll();
		
		domainRepo.deleteAll();
		
		policyGroupRepo.deleteAll();
		
		policyRepo.deleteAll();
		
		certRepo.deleteAll();
		
		dnsRepo.deleteAll();
		
		settingRepo.deleteAll();
	}
}
