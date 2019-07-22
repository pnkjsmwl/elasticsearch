package com.es;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SpringBootApplication
@Configuration
public class AwsElasticsearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(AwsElasticsearchApplication.class, args);
	}

	
	@Bean
	public Gson gson()
	{
		return new GsonBuilder().setPrettyPrinting().create();
	}
}
