package com.test.teamlog;

import com.test.teamlog.config.FileConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@EnableConfigurationProperties({
		FileConfig.class
})
public class TeamlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeamlogApplication.class, args);
	}

}
