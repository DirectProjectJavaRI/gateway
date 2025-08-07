package org.nhindirect.stagent;

import static org.mockito.Mockito.mock;

import org.nhindirect.gateway.smtp.SmtpAgent;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableTestBinder
public class TestApplication
{
    public static void main(String[] args) 
    {
    	new SpringApplicationBuilder(TestApplication.class).web(WebApplicationType.REACTIVE).run(args);
    }  
    
    @Bean
    @ConditionalOnMissingBean
    HttpMessageConverters httpMessageConverters()
    {
    	return new HttpMessageConverters();
    }
    
    @Bean
    ReactiveWebServerFactory reactiveWebServerFactory() {
        return new NettyReactiveWebServerFactory();
    }
    
    @ConditionalOnMissingBean
    @Bean
    SmtpAgent mockSmtpAgent() {
    	
    	return mock(SmtpAgent.class);
    }
    
}
