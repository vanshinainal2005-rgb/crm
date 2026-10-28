package com.enterprise.crm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise CRM & Operations Management API")
                        .version("1.0.0")
                        .description("Production-grade Enterprise CRM RESTful APIs with Oracle Database 26ai Cloud and PL/SQL Business Logic.")
                        .contact(new Contact().name("CRM Enterprise Architecture Team").email("architecture@enterprisecrm.io"))
                        .license(new License().name("Enterprise Commercial License")));
    }
}
