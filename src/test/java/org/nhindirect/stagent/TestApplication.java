package org.nhindirect.stagent;

import org.nhindirect.gateway.springconfig.KeyStoreProtectionMgrConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@ComponentScan({"org.nhindirect.config", "org.nhind.config"})
@EnableFeignClients({"org.nhind.config.rest.feign"})
@EnableR2dbcRepositories("org.nhindirect.config.repository")
@Import({StreamsConfiguration.class, KeyStoreProtectionMgrConfig.class})
public class TestApplication
{
    public static void main(String[] args) 
    {
        SpringApplication.run(TestApplication.class, args);
    }  
    
    @Bean
    @ConditionalOnMissingBean
    public HttpMessageConverters httpMessageConverters()
    {
    	return new HttpMessageConverters();
    }
}
