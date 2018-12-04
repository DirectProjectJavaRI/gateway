package org.nhindirect.gateway.smtp.james.mailet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.mailet.MailetConfig;
import org.junit.Test;
import org.nhindirect.gateway.smtp.dsn.impl.AbstractDSNCreator;
import org.nhindirect.gateway.smtp.dsn.impl.RejectedRecipientDSNCreator;
import org.nhindirect.gateway.testutils.BaseTestPlan;
import org.nhindirect.gateway.testutils.TestUtils;
import org.nhindirect.stagent.SpringBaseTest;

public class NHINDSecurityAndTrustMailet_createDSNCreatorTest extends SpringBaseTest
{
	
	abstract class TestPlan extends BaseTestPlan 
	{		
		
		protected MailetConfig getMailetConfig() throws Exception
		{
			TestUtils.createGatewayConfig(TestUtils.VALID_GATEWAY_CONFIG, settingService, domainService);
			Map<String,String> params = new HashMap<String, String>();
			
			
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
	
	@Test
	public void testCreateDSNCreator_assertDSNCreator() throws Exception 
	{
		new TestPlan() 
		{
			@Override
			protected void doAssertions(NHINDSecurityAndTrustMailet mailet) throws Exception
			{
				assertNotNull(mailet.dsnCreator);
				assertTrue(mailet.dsnCreator instanceof RejectedRecipientDSNCreator);
				RejectedRecipientDSNCreator creator = (RejectedRecipientDSNCreator)mailet.dsnCreator;
				
				
				Field field = AbstractDSNCreator.class.getDeclaredField("mailet");
				field.setAccessible(true);
				Object mailetField = field.get(creator);
				assertNotNull(mailetField);
			}				
		}.perform();
	}
	
}
