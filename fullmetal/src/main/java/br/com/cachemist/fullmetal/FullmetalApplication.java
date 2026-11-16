package br.com.cachemist.fullmetal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
//@EnableScheduling liga os métodos anotados com @Scheduled (rotina de limpeza do cache)
@EnableScheduling
public class FullmetalApplication {

	public static void main(String[] args) {
		SpringApplication.run(FullmetalApplication.class, args);
	}

}
