package org.nhindirect.stagent;

import org.nhindirect.gateway.springconfig.KeyStoreProtectionMgrConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan({"org.nhindirect.config", "org.nhind.config"})
@EnableFeignClients({"org.nhind.config.rest.feign"})
@Import({StreamsConfiguration.class, KeyStoreProtectionMgrConfig.class})
public class TestApplication
{
    public static void main(String[] args) 
    {
        SpringApplication.run(TestApplication.class, args);
    }  
}
