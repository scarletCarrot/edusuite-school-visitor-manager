package com.edusuite.visitors;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class VisitorsApplication {

	/** Single source of "now" and "today", in the school's time zone. */
	@Bean
	Clock clock(VisitorProperties properties) {
		return Clock.system(properties.zone());
	}

	public static void main(String[] args) {
		SpringApplication.run(VisitorsApplication.class, args);
	}

}
