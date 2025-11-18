package com.example.fixparser.report;

import com.example.fixreport.FixReportApp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ReportConfig {

    @Bean
    public FixReportApp fixReportApp(DataSource dataSource) {
        return new FixReportApp(dataSource);
    }
}
