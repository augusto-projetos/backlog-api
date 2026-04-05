package com.augustoprojetos.backlogapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BacklogApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BacklogApiApplication.class, args);
	}

}
