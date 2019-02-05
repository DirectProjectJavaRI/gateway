/* 
Copyright (c) 2010, NHIN Direct Project
All rights reserved.

Authors:
   Umesh Madan     umeshma@microsoft.com
   Greg Meyer      gm2552@cerner.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.  Neither the name of the The NHIN Direct Project (nhindirect.org). 
nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.nhindirect.gateway.smtp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.nhind.config.rest.AnchorService;
import org.nhind.config.rest.CertPolicyService;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.rest.DomainService;
import org.nhind.config.rest.SettingService;
import org.nhind.config.rest.TrustBundleService;
import org.nhindirect.common.audit.Auditor;
import org.nhindirect.common.crypto.KeyStoreProtectionManager;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.model.CertPolicyGroupDomainReltn;
import org.nhindirect.config.model.CertPolicyGroupUse;
import org.nhindirect.config.model.CertPolicyUse;
import org.nhindirect.config.model.Setting;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.model.TrustBundleAnchor;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.gateway.smtp.config.cert.impl.ConfigServiceRESTCertificateStore;
import org.nhindirect.policy.PolicyExpression;
import org.nhindirect.policy.PolicyLexicon;
import org.nhindirect.policy.PolicyLexiconParser;
import org.nhindirect.policy.PolicyLexiconParserFactory;
import org.nhindirect.policy.PolicyParseException;
import org.nhindirect.stagent.DefaultNHINDAgent;
import org.nhindirect.stagent.MutableAgent;
import org.nhindirect.stagent.NHINDAgent;
import org.nhindirect.stagent.cert.CertificateResolver;
import org.nhindirect.stagent.cert.impl.DNSCertificateStore;
import org.nhindirect.stagent.cert.impl.EmployLdapAuthInformation;
import org.nhindirect.stagent.cert.impl.KeyStoreCertificateStore;
import org.nhindirect.stagent.cert.impl.LDAPCertificateStore;
import org.nhindirect.stagent.cert.impl.LdapCertificateStoreFactory;
import org.nhindirect.stagent.cert.impl.LdapPublicCertUtilImpl;
import org.nhindirect.stagent.cert.impl.LdapStoreConfiguration;
import org.nhindirect.stagent.cert.impl.TrustAnchorCertificateStore;
import org.nhindirect.stagent.cert.impl.UniformCertificateStore;
import org.nhindirect.stagent.cryptography.SMIMECryptographerImpl;
import org.nhindirect.stagent.policy.PolicyResolver;
import org.nhindirect.stagent.policy.impl.DomainPolicyResolver;
import org.nhindirect.stagent.trust.DefaultTrustAnchorResolver;
import org.nhindirect.stagent.trust.TrustAnchorResolver;
import org.nhindirect.stagent.trust.TrustModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SmtpAgentFactory is a bootstrapper for creating instances of the {@link SmtpAgent) based on configuration information.  Configurations
 * are loaded from a URL that may take the form of any addressable resource such as a file, HTTP resource, LDAP store, or database.  Based on the
 * URL protocol, an appropriate configuration loader and parser is instantiated which creates an injector used to provide instance of the SmptAgent.
 * Optionally specific configuration and security and trust agent providers can be passed for specific object creation.  This is generally useful
 * for creating mock implementations for testing.
 * @author Greg Meyer
 *
 */
public class SmtpAgentFactory 
{		
	
	protected static SmtpAgentFactory INSTANCE;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SmtpAgentFactory.class);	

	protected static final String MESSAGE_SETTING_RAW = "Raw";
	protected static final String MESSAGE_SETTING_INCOMING = "Incoming";
	protected static final String MESSAGE_SETTING_OUTGOING = "Outgoing";
	protected static final String MESSAGE_SETTING_BAD = "Bad";	
	
	protected static final String ANCHOR_RES_TYPE_UNIFORM = "uniform";
	protected static final String ANCHOR_RES_TYPE_MULTIDOMAIN = "multidomain";	
	
	protected static final String STORE_TYPE_WS = "WS";
	protected static final String STORE_TYPE_LDAP = "LDAP";
	protected static final String STORE_TYPE_PUBLIC_LDAP = "PublicLDAP";
	protected static final String STORE_TYPE_KEYSTORE = "keystore";
	protected static final String STORE_TYPE_DNS = "DNS";	
	
	
	protected final CertificateService certService;
	protected final TrustBundleService bundleService;
	protected final DomainService domainService;
	protected final AnchorService anchorService;
	protected final SettingService settingService;
	protected final CertPolicyService polService;
	protected final Auditor auditor;
	protected final KeyStoreProtectionManager keyStoreMgr;
	
	public static SmtpAgentFactory getInstance(CertificateService certService, TrustBundleService bundleService, DomainService domainService,
			AnchorService anchorService, SettingService settingService, CertPolicyService polService, Auditor auditor, KeyStoreProtectionManager keyStoreMgr)
	{

		INSTANCE = new SmtpAgentFactory(certService, bundleService, domainService,
					anchorService, settingService, polService, auditor, keyStoreMgr);
		
		return INSTANCE;
	}
	
	protected SmtpAgentFactory(CertificateService certService, TrustBundleService bundleService, DomainService domainService,
			AnchorService anchorService, SettingService settingService, CertPolicyService polService, Auditor auditor, KeyStoreProtectionManager keyStoreMgr)
	{
		this.certService = certService;
		this.bundleService = bundleService;
		this.domainService = domainService;
		this.anchorService = anchorService;
		this.settingService = settingService;
		this.polService = polService;
		this.auditor = auditor;
		this.keyStoreMgr = keyStoreMgr;
	}
	
	public SmtpAgent createSmtpAgent() throws SmtpAgentException
	{
		// get the settings
		final SmtpAgentSettings settings = createSMTPAgentSetting();
		
		// create the agent
		final SmtpAgent retVal = new DefaultSmtpAgent(settings, createNHINDAgent(), auditor);
		
		return retVal;
	}
	
	public NHINDAgent createNHINDAgent() throws SmtpAgentException
	{
		final List<String> domains = getDomains();
		
		final TrustAnchorResolver anchorResover = getTrustAnchorResolver(domains);
		
		final Collection<CertificateResolver> publicResolvers = getPublicCertResolvers();
		
		final CertificateResolver privateResolver = getPrivateCertResolver();
		
		final PolicyResolvers polResolvers = this.getPolicyResolvers();
		
		final MutableAgent retVal = new DefaultNHINDAgent(domains, privateResolver, publicResolvers, anchorResover, 
				TrustModel.Default, SMIMECryptographerImpl.Default);
		
		retVal.setPrivatePolicyResolver(polResolvers.getPrivateResolver());
		retVal.setPublicPolicyResolver(polResolvers.getPublicResolver());
		retVal.getTrustModel().setTrustPolicyResolver(polResolvers.getTrustResolver());
		
		return (NHINDAgent)retVal;
	}	
	

	protected SmtpAgentSettings createSMTPAgentSetting()
	{
		final MessageProcessingSettings rawSetting = getMessageProcessingSetting(MESSAGE_SETTING_RAW + "MessageSaveFolder");
		final MessageProcessingSettings outgoingSettings = getMessageProcessingSetting(MESSAGE_SETTING_OUTGOING + "MessageSaveFolder");
		final MessageProcessingSettings incomingSettings = getMessageProcessingSetting(MESSAGE_SETTING_INCOMING + "MessageSaveFolder");
		final MessageProcessingSettings badSettings = getMessageProcessingSetting(MESSAGE_SETTING_BAD + "MessageSaveFolder");
		
		final Setting prodNameSetting = getSafeSetting("MDNProdName");
		final Setting textSetting = getSafeSetting("MDNText");
		boolean autoResponse = true;  /* MDNs are part of the Direct spec.  This should not be an option */
		final String prodName = (prodNameSetting == null) ? "" : prodNameSetting.getValue();
		final String text = (textSetting == null) ? "" : textSetting.getValue();
		final NotificationProducer notificationProducer = new NotificationProducer(new NotificationSettings(autoResponse, prodName, text));
		
		return new SmtpAgentSettings(rawSetting, outgoingSettings,
				incomingSettings, badSettings, notificationProducer);
				
				
	}

	protected List<String> getDomains()
	{
		List<String> domains = null;

		try
		{
			domains = domainService.searchDomains("", null).stream().map(domain -> domain.getDomainName())
				.collect(Collectors.toList());
		}
		catch (Exception e)
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting domains list: " + e.getMessage(), e);
		}
		
		if (domains.size() == 0)
			throw new SmtpAgentException(SmtpAgentError.MissingDomains);
		
		return domains;
	}
	
	
	protected TrustAnchorResolver getTrustAnchorResolver(List<String> domains) 
	{

		final Map<String, Collection<X509Certificate>> incomingAnchors = new HashMap<>();
		final Map<String, Collection<X509Certificate>> outgoingAnchors = new HashMap<>();
		
		/* 
		 * first determine how anchors are stored... possibilities are LDAP, keystore, and WS
		 * 
		 */
		Setting setting = null;
		String storeType;
		String resolverType;
		try
		{
			setting = getSafeSetting("AnchorStoreType");
		}
		catch (Exception e)
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting anchor store type: " + e.getMessage(), e);
		}
		
		if (setting == null || setting.getValue() == null || setting.getValue().isEmpty())
			storeType = STORE_TYPE_WS; // default to WS
		else
			storeType = setting.getValue();		
		
		if (!storeType.equalsIgnoreCase(STORE_TYPE_WS))
		{
			getAnchorsFromNonWS(incomingAnchors, outgoingAnchors, storeType, domains);
		}				
		else
		{
			Map<String, TrustBundle> bundleMap = null;
			
			/*
			 * Get all the of the trust bundles and put them into a map
			 * keyed by the bundle name.  We will use this later
			 * when populating each domain's collection of anchors.
			 */
			try
			{
				bundleMap = bundleService.getTrustBundles(true).
						stream().collect(Collectors.toMap(TrustBundle::getBundleName, Function.identity()));
			}
			catch (Exception e)
			{
				throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat,  
						"WebService error getting trust bundles: " + e.getMessage(), e);
			}
	
			final Map<String, TrustBundle> finalBundleMap = Collections.unmodifiableMap(bundleMap);
			
			/*
			 * Get all of the anchors in the system in one call.  This is more efficient
			 * than making numerous REST and database calls.  
			 */
			Collection<Anchor> systemAnchors = null;
			try
			{
				systemAnchors = anchorService.getAnchors();
			}
			catch (Exception e)
			{
				throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat,  
						"WebService error getting anchors: " + e.getMessage(), e);
			}
			
			/*
			 * Iterate through all of the domains and start them out with an empty collection.
			 * We will fill in the collections as we go in the next sections
			 */
			for (String domain : domains)
			{
				incomingAnchors.put(domain, new ArrayList<X509Certificate>());
				outgoingAnchors.put(domain, new ArrayList<X509Certificate>());
			}

			/*
			 * Iterate through all anchors and drop them into their
			 * appropriate domain collections
			 */
			systemAnchors.forEach(anchor -> 
				{
					final X509Certificate anchorToAdd = CertUtils.toX509Certificate(anchor.getCertificateData());
					if (anchor.isIncoming())
						incomingAnchors.get(anchor.getOwner()).add(anchorToAdd);
					if (anchor.isOutgoing())
						outgoingAnchors.get(anchor.getOwner()).add(anchorToAdd);					
				});
			
			try
			{
				/*
				 * Get all of the trust bundle to domain relation ships.  We will lookup
				 * the anchors in the bundles using the bundle map that we retrieved earlier.
				 * Then drop the anchors from each bundle into it appropriate domain collection
				 */
				bundleService.getAllTrustBundleDomainReltns(false).stream().forEach(domainAssoc ->
				{
					final TrustBundle bundle = finalBundleMap.get(domainAssoc.getTrustBundle().getBundleName());
					if (bundle != null && bundle.getTrustBundleAnchors() != null)
					{
						for (TrustBundleAnchor anchor : bundle.getTrustBundleAnchors())
						{
							final X509Certificate anchorToAdd = CertUtils.toX509Certificate(anchor.getAnchorData());
							if (domainAssoc.isIncoming())
								incomingAnchors.get(domainAssoc.getDomain().getDomainName()).add(anchorToAdd);
							if (domainAssoc.isOutgoing())
								outgoingAnchors.get(domainAssoc.getDomain().getDomainName()).add(anchorToAdd);
						}
					}					
				});
			}
			catch (Exception e)
			{
				throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat,  
						"WebService error getting trust bundle/domain relationships: " + e.getMessage(), e);
			}
					

		}
		
		if (incomingAnchors.size() == 0 && outgoingAnchors.size() == 0)
			throw new SmtpAgentException(SmtpAgentError.InvalidTrustAnchorSettings, "No trust anchors defined.");		
		
		try
		{
			setting = getSafeSetting("AnchorResolverType");
		}
		catch (Exception e)
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting anchor resolver type: " + e.getMessage(), e);
		}		
		
		if (setting == null || setting.getValue() == null || setting.getValue().isEmpty())
		{
			// multi domain should be the default... uniform really only makes sense for dev purposes
			resolverType = ANCHOR_RES_TYPE_MULTIDOMAIN; 		
		}
		else
			resolverType = setting.getValue();		
		
		if (resolverType.equalsIgnoreCase(ANCHOR_RES_TYPE_UNIFORM))
		{
			// this is uniform... doesn't really matter what we use for incoming or outgoing because in theory they should be
			// the same... just get the first collection in the incoming map
			final Collection<X509Certificate> anchorsToUse = (incomingAnchors.size() > 0) ? incomingAnchors.values().iterator().next() 
					: outgoingAnchors.values().iterator().next();
			
			return new DefaultTrustAnchorResolver(new UniformCertificateStore(anchorsToUse));
		}
		else if (resolverType.equalsIgnoreCase(ANCHOR_RES_TYPE_MULTIDOMAIN))
		{
			final TrustAnchorCertificateStore incomingTrustStore = new TrustAnchorCertificateStore(incomingAnchors);
			final TrustAnchorCertificateStore outgoingTrustStore = new TrustAnchorCertificateStore(outgoingAnchors);
			
			return new DefaultTrustAnchorResolver(outgoingTrustStore, incomingTrustStore);
		}
		else
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidTrustAnchorSettings);
		}		
	}
	
	protected void getAnchorsFromNonWS(Map<String, Collection<X509Certificate>> incomingAnchors, 
			Map<String, Collection<X509Certificate>> outgoingAnchors, String storeType, Collection<String> domains)
	{		
		

		ArrayList<String> incomingLookups = new ArrayList<String>();
		ArrayList<String> outgoingLookups = new ArrayList<String>();
		for (String domain : domains)
		{
			incomingLookups.add(domain + "IncomingAnchorAliases");
			outgoingLookups.add(domain + "OutgoingAnchorAliases");
		}
		
		Collection<Setting> incomingAliasSettings = new ArrayList<Setting>();
		Collection<Setting> outgoingAliasSettings = new ArrayList<Setting>();
		for (String lookup : incomingLookups)
		{
			try
			{
				Setting st = getSafeSetting(lookup);
				if (st != null)
					incomingAliasSettings.add(st);
			}
			catch (Exception e)
			{
				throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting anchor aliases: " + e.getMessage(), e);
			}
		}
		
		for (String lookup : outgoingLookups)
		{
			try
			{
				Setting st = getSafeSetting(lookup);
				if (st != null)
					outgoingAliasSettings.add(st);
			}
			catch (Exception e)
			{
				throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting anchor aliases: " + e.getMessage(), e);
			}
		}
		
		// get the anchors from the correct store
		if (storeType.equalsIgnoreCase(STORE_TYPE_KEYSTORE))
		{
			Setting file;
			Setting pass;
			Setting privKeyPass;
			try
			{
				file = getSafeSetting("AnchorKeyStoreFile");
				pass = getSafeSetting("AnchorKeyStoreFilePass");
				privKeyPass = getSafeSetting("AnchorKeyStorePrivKeyPass");
			}
			catch (Exception e)
			{
				throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting anchor key store settings: " + e.getMessage(), e);
			}
			
			final KeyStoreCertificateStore store = new KeyStoreCertificateStore((file == null) ? null : file.getValue(), 
					(pass == null) ? "DefaultFilePass" : pass.getValue(), (privKeyPass == null) ? "DefaultKeyPass" : privKeyPass.getValue());
			
			// get incoming anchors
			if (incomingAliasSettings != null)
			{
				for (Setting setting : incomingAliasSettings)				
				{
					Collection<X509Certificate> certs = new ArrayList<X509Certificate>();				
					String aliases[] = setting.getValue().split(",");
					for (String alias : aliases)
					{
						X509Certificate cert = store.getByAlias(alias);
						if (cert != null)
						{
							certs.add(cert);
						}
					}				
					incomingAnchors.put(setting.getName().substring(0, setting.getName().lastIndexOf("IncomingAnchorAliases")), certs);
				}
			}
			
			// get outgoing anchors
			if (outgoingAliasSettings != null)
			{
				for (Setting setting : outgoingAliasSettings)				
				{
					Collection<X509Certificate> certs = new ArrayList<X509Certificate>();
					String aliases[] = setting.getValue().split(",");
					for (String alias : aliases)
					{
						X509Certificate cert = store.getByAlias(alias);
						if (cert != null)
						{
							certs.add(cert);
						}
					}				
					outgoingAnchors.put(setting.getName().substring(0, setting.getName().lastIndexOf("OutgoingAnchorAliases")), certs);
				}
			}
		}
		else
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "Unknow anchor store type: " + storeType);
		}
	}	
	
	protected Collection<CertificateResolver> getPublicCertResolvers() 
	{
		final Collection<CertificateResolver> resolvers = new ArrayList<>();
		
		Setting setting = null;
		String storeTypes;
		try
		{
			setting = getSafeSetting("PublicStoreType");
		}
		catch (Exception e)
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting public store type: " + e.getMessage(), e);
		}	
		
		if (setting == null || setting.getValue() == null || setting.getValue().isEmpty())
			storeTypes = STORE_TYPE_DNS + "," + STORE_TYPE_PUBLIC_LDAP; // default to DNS,LDAP
		else
			storeTypes = setting.getValue();
		
		final String[] types = storeTypes.split(",");
		CertificateResolver lookedUpResolver = null;
		for (String storeType : types)
		{
			/*
			 * Keystore
			 */
			if (storeType.equalsIgnoreCase(STORE_TYPE_KEYSTORE))
			{
				Setting file;
				Setting pass;
				Setting privKeyPass;
				try
				{
					file = getSafeSetting("PublicStoreFile");
					pass = getSafeSetting("PublicStoreFilePass"); 
					privKeyPass = getSafeSetting("PublicStorePrivKeyPass"); 
				}
				catch (Exception e)
				{
					throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting public store file settings: " + e.getMessage(), e);
				}
				
				lookedUpResolver = new KeyStoreCertificateStore((file == null) ? "PublicStoreKeyFile" : file.getValue(), 
						(pass == null) ? "DefaultFilePass" : pass.getValue(), (privKeyPass == null) ? "DefaultKeyPass" : privKeyPass.getValue());
			}
			/*
			 * DNS resolver
			 */			
			else if(storeType.equalsIgnoreCase(STORE_TYPE_DNS))
			{
				lookedUpResolver = new DNSCertificateStore(Collections.emptyList(), null, new DNSCertificateStore.DefaultDNSCachePolicy());								
			}
			/*
			 * Config Service
			 */
			else if (storeType.equalsIgnoreCase(STORE_TYPE_WS))
			{
				lookedUpResolver = new ConfigServiceRESTCertificateStore(certService, 
						null, new ConfigServiceRESTCertificateStore.DefaultConfigStoreCachePolicy(), keyStoreMgr);
			}
			/*
			 * Public LDAP resolver
			 */
			else if (storeType.equalsIgnoreCase(STORE_TYPE_PUBLIC_LDAP))
			{
				lookedUpResolver = new LDAPCertificateStore(new LdapPublicCertUtilImpl(), null, 
						new LDAPCertificateStore.DefaultLDAPCachePolicy());
			}
			/*
			 * Default to DNS with a default cache policy
			 */
			else
			{
				lookedUpResolver = new DNSCertificateStore(Collections.emptyList(), null, new DNSCertificateStore.DefaultDNSCachePolicy());				
			}		
			
			resolvers.add(lookedUpResolver);
		}
		
		
		return resolvers;
	}
	
	protected CertificateResolver getPrivateCertResolver() 
	{
		CertificateResolver resolver = null;
		
		Setting setting = null;
		String storeType;
		try
		{
			setting = getSafeSetting("PrivateStoreType"); 
		}
		catch (Exception e)
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting private store type: " + e.getMessage(), e);
		}			
		
		if (setting == null || setting.getValue() == null || setting.getValue().isEmpty())
			storeType = STORE_TYPE_WS; // default to WS
		else
			storeType = setting.getValue();	
		
		if (storeType.equalsIgnoreCase(STORE_TYPE_KEYSTORE))
		{
			Setting file;
			Setting pass;
			Setting privKeyPass;
			try
			{
				file = getSafeSetting("PrivateStoreFile");
				pass = getSafeSetting("PrivateStoreFilePass"); 
				privKeyPass = getSafeSetting("PrivateStorePrivKeyPass"); 
			}
			catch (Exception e)
			{
				throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting public store file settings: " + e.getMessage(), e);
			}
			
			resolver = new KeyStoreCertificateStore((file == null) ? "PublicStoreKeyFile" : file.getValue(), 
					(pass == null) ? "DefaultFilePass" : pass.getValue(), (privKeyPass == null) ? "DefaultKeyPass" : privKeyPass.getValue());
		}		
		else if(storeType.equalsIgnoreCase(STORE_TYPE_LDAP))
		{
			resolver = getPrivateLdapCertificateStore("PrivateStore", "LDAPPrivateCertStore");
		}
		else if (storeType.equalsIgnoreCase(STORE_TYPE_WS))
		{
			resolver = new ConfigServiceRESTCertificateStore(certService, 
					null, new ConfigServiceRESTCertificateStore.DefaultConfigStoreCachePolicy(), keyStoreMgr);
		}
		else
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidPrivateCertStoreSettings);
		}	
		
		return resolver;
	}
	
	protected MessageProcessingSettings getMessageProcessingSetting(String settingName)
	{
		final MessageProcessingSettings retVal = new MessageProcessingSettings();
		try
		{
			final Setting setting = getSafeSetting(settingName);
			
			if (setting != null && !StringUtils.isEmpty(setting.getValue()))
				retVal.setSaveMessageFolder(new File(setting.getValue()));
		}
		catch (Exception e)
		{
			LOGGER.warn("Could not get setting " + settingName, e);
		}
		
		return retVal;
	}
	
	protected Setting getSafeSetting(String settingName)
	{
		try
		{
			return settingService.getSetting(settingName);
		}
		catch (Exception e)
		{
			LOGGER.info("Could not get setting " + settingName);
			return null;
		}
	}
	
	protected CertificateResolver getPrivateLdapCertificateStore(String type, String cacheStoreName)
	{
	    //required
		Setting ldapURLSetting;
		Setting ldapSearchBaseSetting;
		Setting ldapSearchAttrSetting;
		Setting ldapCertAttrSetting;
		Setting ldapCertFormatSetting;
        //optional	    
	    Setting ldapUserSetting;
	    Setting ldapPasswordSetting;
	    Setting ldapConnTimeoutSetting;	   
	    Setting ldapCertPassphraseSetting;	
		try
		{
			ldapURLSetting = getSafeSetting(type +  "LDAPUrl"); 
			ldapSearchBaseSetting = getSafeSetting(type +  "LDAPSearchBase"); 
			ldapSearchAttrSetting = getSafeSetting(type +  "LDAPSearchAttr"); 
			ldapCertAttrSetting = getSafeSetting(type +  "LDAPCertAttr"); 
			ldapCertFormatSetting = getSafeSetting(type +  "LDAPCertFormat"); 
	        //optional	    
		    ldapUserSetting = getSafeSetting(type +  "LDAPUser"); 
		    ldapPasswordSetting =  getSafeSetting(type +  "LDAPPassword"); 
		    ldapConnTimeoutSetting =  getSafeSetting(type +  "LDAPConnTimeout");   
		    ldapCertPassphraseSetting =  getSafeSetting(type +  "LDAPCertPassphrase"); 
		}
		catch (Exception e)
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting LDAP store settings: " + e.getMessage(), e);
		}
        if (ldapURLSetting == null || ldapURLSetting.getValue() == null || ldapURLSetting.getValue().isEmpty())
        	 throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "Missing LDAP URL");
        
		String ldapSearchBase = (ldapSearchBaseSetting == null) ? null : ldapSearchBaseSetting.getValue();
		String ldapSearchAttr = (ldapSearchAttrSetting == null) ? null : ldapSearchAttrSetting.getValue();
		String ldapCertAttr = (ldapCertAttrSetting == null) ? null : ldapCertAttrSetting.getValue();
		String ldapCertFormat = (ldapCertFormatSetting == null) ? null : ldapCertFormatSetting.getValue();
        String[] ldapURL = ldapURLSetting.getValue().split(",");

        if(ldapURL[0].isEmpty() || ldapSearchBase.isEmpty() || ldapSearchAttr.isEmpty() ||
                ldapCertAttr.isEmpty() || ldapCertFormat.isEmpty())
        {
            throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "Missing required LDAP parameters.");
        }        
        	    
	    String ldapUser = (ldapUserSetting == null) ? null : ldapUserSetting.getValue();
	    String ldapPassword =  (ldapPasswordSetting == null) ? null : ldapPasswordSetting.getValue();
	    String ldapConnTimeout =  (ldapConnTimeoutSetting == null) ? null : ldapConnTimeoutSetting.getValue();	   
	    String ldapCertPassphrase =  (ldapCertPassphraseSetting == null) ? null : ldapCertPassphraseSetting.getValue();    
	    
	    
	    if(ldapCertFormat.equalsIgnoreCase("pkcs12") && ( ldapCertPassphrase == null || ldapCertPassphrase.isEmpty()))
	    {
	        throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat);
	    }
	    LdapStoreConfiguration ldapStoreConfiguration = new LdapStoreConfiguration(ldapURL, ldapSearchBase, ldapSearchAttr, ldapCertAttr, ldapCertFormat);
	    if(ldapUser != null && !ldapUser.isEmpty() && ldapPassword != null && !ldapPassword.isEmpty())
	    {
	        ldapStoreConfiguration.setEmployLdapAuthInformation(new EmployLdapAuthInformation(ldapUser, ldapPassword));
	    }
	    if(ldapConnTimeout != null && !ldapConnTimeout.isEmpty())
	    {
	        ldapStoreConfiguration.setLdapConnectionTimeOut(ldapConnTimeout);
	    }
	    if(ldapCertPassphrase != null && !ldapCertPassphrase.isEmpty())
	    {
	        ldapStoreConfiguration.setLdapCertPassphrase(ldapCertPassphrase);
	    }
	    
	    return LdapCertificateStoreFactory.createInstance(ldapStoreConfiguration, null, new LDAPCertificateStore.DefaultLDAPCachePolicy());
	}	
	
	protected PolicyResolvers getPolicyResolvers()
	{
		final Map<String, Collection<PolicyExpression>> incomingPrivatePolicies = new HashMap<>();
		final Map<String, Collection<PolicyExpression>> outgoingPrivatePolicies = new HashMap<>();
		
		final Map<String, Collection<PolicyExpression>> incomingPublicPolicies = new HashMap<>();
		final Map<String, Collection<PolicyExpression>> outgoingPublicPolicies = new HashMap<>();
	
		final Map<String, Collection<PolicyExpression>> trustPolicies = new HashMap<>();
		
		Collection<CertPolicyGroupDomainReltn> domainReltns = null;
		try
		{   
			// get all of the policy group to domain relations... 
			// doing this all in one call for efficiency
			domainReltns = polService.getPolicyGroupDomainReltns();
		}
		catch (Exception e)
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "WebService error getting certificate policy configuration: " + e.getMessage(), e);
		}
		
		if (domainReltns != null)
		{
			for (CertPolicyGroupDomainReltn domainReltn : domainReltns)
			{
				if (domainReltn.getPolicyGroup().getPolicies() != null)
				{
					for (CertPolicyGroupUse policyReltn : domainReltn.getPolicyGroup().getPolicies())
					{						
						if (policyReltn.getPolicyUse().equals(CertPolicyUse.PRIVATE_RESOLVER))
						{
							if (policyReltn.isIncoming())
								addPolicyToMap(incomingPrivatePolicies, domainReltn.getDomain().getDomainName(), policyReltn);
							if (policyReltn.isOutgoing())
								addPolicyToMap(outgoingPrivatePolicies, domainReltn.getDomain().getDomainName(), policyReltn);
						}
						else if (policyReltn.getPolicyUse().equals(CertPolicyUse.PUBLIC_RESOLVER))
						{
							if (policyReltn.isIncoming())
								addPolicyToMap(incomingPublicPolicies, domainReltn.getDomain().getDomainName(), policyReltn);
							if (policyReltn.isOutgoing())
								addPolicyToMap(outgoingPublicPolicies, domainReltn.getDomain().getDomainName(), policyReltn);							
						}
						else if (policyReltn.getPolicyUse().equals(CertPolicyUse.TRUST))
						{
							addPolicyToMap(trustPolicies, domainReltn.getDomain().getDomainName(), policyReltn);
						}	
					}
				}
			}
		}
		
		final PolicyResolvers retVal = new PolicyResolvers(new DomainPolicyResolver(incomingPublicPolicies, outgoingPublicPolicies), 
				new DomainPolicyResolver(incomingPrivatePolicies, outgoingPrivatePolicies),
				new DomainPolicyResolver(trustPolicies, trustPolicies));
		
		return retVal;
	}	
	
	
	@SuppressWarnings("deprecation")
	public void addPolicyToMap(Map<String, Collection<PolicyExpression>> policyMap, String domainName, CertPolicyGroupUse policyReltn)
	{
		// check to see if the domain is in the map
		Collection<PolicyExpression> policyExpressionCollection = policyMap.get(domainName);
		if (policyExpressionCollection == null)
		{
			policyExpressionCollection = new ArrayList<PolicyExpression>();
			policyMap.put(domainName, policyExpressionCollection);
		}
		
		final CertPolicy policy = policyReltn.getPolicy();
		final PolicyLexicon lexicon = policy.getLexicon();
		
		final InputStream inStr = new ByteArrayInputStream(policy.getPolicyData());
		
		try
		{
			// grab a parser and compile this policy
			final PolicyLexiconParser parser = PolicyLexiconParserFactory.getInstance(lexicon);
			
			policyExpressionCollection.add(parser.parse(inStr));
		}
		catch (PolicyParseException ex)
		{
			throw new SmtpAgentException(SmtpAgentError.InvalidConfigurationFormat, "Failed parse policy into policy expression: " + ex.getMessage(), ex);
		}
		finally
		{
			IOUtils.closeQuietly(inStr);
		}
		
	}
	
	protected static class PolicyResolvers
	{
		private final PolicyResolver publicResolver;
		private final PolicyResolver privateResolver;
		private final PolicyResolver trustResolver;
		
		public PolicyResolvers(PolicyResolver publicResolver, PolicyResolver privateResolver, PolicyResolver trustResolver)
		{
			this.publicResolver = publicResolver;
			this.privateResolver = privateResolver;
			this.trustResolver = trustResolver;
		}
		
		public PolicyResolver getPublicResolver()
		{
			return publicResolver;
		}

		public PolicyResolver getPrivateResolver()
		{
			return privateResolver;
		}

		public PolicyResolver getTrustResolver()
		{
			return trustResolver;
		}
	}
}
