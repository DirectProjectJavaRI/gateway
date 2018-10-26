package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.mailet.MailetConfig;
import org.junit.Test;
import org.nhindirect.common.options.OptionsManager;
import org.nhindirect.common.options.OptionsManagerUtils;
import org.nhindirect.common.options.OptionsParameter;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.SpringBaseTest;


public class NHINDSecurityAndTrustMailet_outboundPolicyForInboundTest extends SpringBaseTest
{
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			TestUtils.createGatewayConfig(getConfigName(), settingService, domainService);
			Map<String,String> params = new HashMap<String, String>();
			

			if (getUseOutboundPolicy() != null)
				params.put("UseOutgoingPolicyForIncomingNotifications", getUseOutboundPolicy());
			
			return new MockMailetConfig(params, "NHINDSecurityAndTrustMailet");	
		}
		
		@Override
		protected void setupMocks() 
		{
			OptionsManagerUtils.clearOptionsManagerInstance();
		}
		
		@Override
		protected void tearDownMocks()
		{
			OptionsManagerUtils.clearOptionsManagerOptions();
			OptionsManagerUtils.clearOptionsManagerInstance();
		}
		
		@Override
		protected void performInner() throws Exception
		{
			NHINDSecurityAndTrustMailet theMailet = new NHINDSecurityAndTrustMailet();

			MailetConfig config = getMailetConfig();
			
			theMailet.init(config);
			doAssertions();
		}
		
		
		protected String getConfigName()
		{
			return TestUtils.VALID_GATEWAY_CONFIG;
		}

		protected String getUseOutboundPolicy()
		{
			return "";
		}
		
		protected void doAssertions() throws Exception
		{
			
		}			
	}
	
	@Test
	public void testOutboundPolicyForInbound_emptyMailetParamAndNullOptions_assertFalse() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected void doAssertions() throws Exception
			{
				final OptionsParameter param = OptionsManager.getInstance().getParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS);
				assertNotNull(param);
				assertEquals("false", param.getParamValue());
						
			}				
		}.perform();
	}
	
	@Test
	public void testOutboundPolicyForInbound_nullMailetParamAndNullOptions_assertFalse() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected String getUseOutboundPolicy()
			{
				return null;
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final OptionsParameter param = OptionsManager.getInstance().getParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS);
				assertNotNull(param);
				assertEquals("false", param.getParamValue());
						
			}				
		}.perform();
	}
	
	@Test
	public void testOutboundPolicyForInbound_falseMailetParamAndNullOptions_assertFalse() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected String getUseOutboundPolicy()
			{
				return "false";
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final OptionsParameter param = OptionsManager.getInstance().getParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS);
				assertNotNull(param);
				assertEquals("false", param.getParamValue());
						
			}				
		}.perform();
	}	
	
	@Test
	public void testOutboundPolicyForInbound_invalidMailetParamAndNullOptions_assertFalse() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected String getUseOutboundPolicy()
			{
				return "bogus";
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final OptionsParameter param = OptionsManager.getInstance().getParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS);
				assertNotNull(param);
				assertEquals("false", param.getParamValue());
						
			}				
		}.perform();
	}	
	
	@Test
	public void testOutboundPolicyForInbound_trueMailetParamAndNullOptions_assertTrue() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			protected String getUseOutboundPolicy()
			{
				return "true";
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final OptionsParameter param = OptionsManager.getInstance().getParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS);
				assertNotNull(param);
				assertEquals("true", param.getParamValue());
						
			}				
		}.perform();
	}
	
	@Test
	public void testOutboundPolicyForInbound_trueMailetParamAndFalseOptions_assertTrue() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			public void setupMocks()
			{
				super.setupMocks();
				
				OptionsManager.getInstance().setOptionsParameter(
						new OptionsParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS, "false"));
			}
			
			@Override
			protected String getUseOutboundPolicy()
			{
				return "true";
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final OptionsParameter param = OptionsManager.getInstance().getParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS);
				assertNotNull(param);
				assertEquals("true", param.getParamValue());
						
			}				
		}.perform();
	}	
	
	@Test
	public void testOutboundPolicyForInbound_falseMailetParamAndTrueOptions_assertFalse() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			public void setupMocks()
			{
				super.setupMocks();
				
				OptionsManager.getInstance().setOptionsParameter(
						new OptionsParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS, "false"));
			}
			
			@Override
			protected String getUseOutboundPolicy()
			{
				return "false";
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final OptionsParameter param = OptionsManager.getInstance().getParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS);
				assertNotNull(param);
				assertEquals("false", param.getParamValue());
						
			}				
		}.perform();
	}	
	
	@Test
	public void testOutboundPolicyForInbound_nullMailetParamAndTrueOptions_assertTrue() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			public void setupMocks()
			{
				super.setupMocks();
				
				OptionsManager.getInstance().setOptionsParameter(
						new OptionsParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS, "true"));
			}
			
			@Override
			protected String getUseOutboundPolicy()
			{
				return null;
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final OptionsParameter param = OptionsManager.getInstance().getParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS);
				assertNotNull(param);
				assertEquals("true", param.getParamValue());
						
			}				
		}.perform();
	}	
	
	@Test
	public void testOutboundPolicyForInbound_nullMailetParamAndFalseOptions_assertFalse() throws Exception 
	{
		new TestPlan() 
		{	
			@Override
			public void setupMocks()
			{
				super.setupMocks();
				
				OptionsManager.getInstance().setOptionsParameter(
						new OptionsParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS, "false"));
			}
			
			@Override
			protected String getUseOutboundPolicy()
			{
				return null;
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final OptionsParameter param = OptionsManager.getInstance().getParameter(OptionsParameter.USE_OUTGOING_POLICY_FOR_INCOMING_NOTIFICATIONS);
				assertNotNull(param);
				assertEquals("false", param.getParamValue());
						
			}				
		}.perform();
	}		
}
