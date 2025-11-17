package com.lunch.micro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.lunch.micro.client")
public class LunchSvcApplication {

	public static void main(String[] args) {

        SpringApplication.run(LunchSvcApplication.class, args);
	}

}
