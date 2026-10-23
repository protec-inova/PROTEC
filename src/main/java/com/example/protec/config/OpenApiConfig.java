package com.example.protec.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI protecOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("PROTEC API")
                .version("0.0.1")
                .description("API do sistema PROTEC - Gestão de Máquinas e Funcionários"));
    }
}
