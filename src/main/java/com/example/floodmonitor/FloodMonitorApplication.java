package com.example.floodmonitor;

import com.example.floodmonitor.worker.StationProcess;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FloodMonitorApplication {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("station")) {
            StationProcess.start(args);
            return;
        }
        SpringApplication.run(FloodMonitorApplication.class, args);
    }
}
