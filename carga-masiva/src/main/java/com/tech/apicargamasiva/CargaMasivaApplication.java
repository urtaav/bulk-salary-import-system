package com.tech.apicargamasiva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CargaMasivaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CargaMasivaApplication.class, args);
	}

}
