package com.projetointegrador.seumentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SeumentorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeumentorApplication.class, args);
	}

}
