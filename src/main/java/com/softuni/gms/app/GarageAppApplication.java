package com.softuni.gms.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.softuni.gms.app.client")
public class GarageAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(GarageAppApplication.class, args);
	}

}
