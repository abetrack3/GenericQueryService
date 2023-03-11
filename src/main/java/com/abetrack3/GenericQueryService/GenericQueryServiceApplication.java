package com.abetrack3.GenericQueryService;

import com.abetrack3.GenericQueryService.Controller.AppRuntimeConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GenericQueryServiceApplication {

	public static void main(String[] args) {
		AppRuntimeConfiguration.resolve();
		SpringApplication.run(GenericQueryServiceApplication.class, args);
	}

}
