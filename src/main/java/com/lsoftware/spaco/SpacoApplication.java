package com.lsoftware.spaco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpacoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpacoApplication.class, args);
	}

}
