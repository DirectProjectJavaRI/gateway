package org.nhindirect.stagent;

import static org.mockito.Mockito.mock;

import org.nhindirect.gateway.smtp.SmtpAgent;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication()
@EnableTestBinder
public class TestApplication
{
    public static void main(String[] args) 
    {
    	new SpringApplicationBuilder(TestApplication.class).web(WebApplicationType.REACTIVE).run(args);
    }  
    
    
    @ConditionalOnMissingBean
    @Bean
    SmtpAgent mockSmtpAgent() {
    	
    	return mock(SmtpAgent.class);
    }
    
}
