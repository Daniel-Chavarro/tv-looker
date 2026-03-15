package org.tvl.tvlooker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class TvLookerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TvLookerApplication.class, args);
    }

}
