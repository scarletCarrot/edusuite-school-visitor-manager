package com.edusuite.visitors;

import org.springframework.boot.SpringApplication;

public class TestVisitorsApplication {

	public static void main(String[] args) {
		SpringApplication.from(VisitorsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
