package com.lunch.micro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableFeignClients(basePackages = "com.lunch.micro.client")
@EnableScheduling
public class LunchSvcApplication {

	public static void main(String[] args) {

        SpringApplication.run(LunchSvcApplication.class, args);
	}

}
