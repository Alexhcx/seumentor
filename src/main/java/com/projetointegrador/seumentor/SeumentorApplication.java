package com.projetointegrador.seumentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableAsync
@EnableMethodSecurity
@EnableJpaAuditing
public class SeumentorApplication {
	public static void main(String[] args) {
		SpringApplication.run(SeumentorApplication.class, args);
	}
}
