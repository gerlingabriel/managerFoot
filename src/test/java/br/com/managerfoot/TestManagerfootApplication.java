package br.com.managerfoot;

import org.springframework.boot.SpringApplication;

public class TestManagerfootApplication {

	public static void main(String[] args) {
		SpringApplication.from(ManagerfootApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
