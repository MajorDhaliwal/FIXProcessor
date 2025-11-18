package com.example.fixparser.report;

import com.example.fixreport.FixReportApp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;


@RestController
@RequestMapping("/api/report")
@Tag(name = "Report API", description = "Endpoints for generating and retrieving FIX reports")
public class ReportController {

    private final FixReportApp reportApp;

    public ReportController(FixReportApp reportApp) {
        this.reportApp = reportApp;
    }

    @GetMapping("/generate")
    public ResponseEntity<String> generateReport() throws Exception {
        String outputPath = "/reports/fix_report.txt";
        reportApp.generateReport(outputPath);
        return ResponseEntity.ok("Report saved to: " + outputPath);
    }

    @GetMapping("/read")
public ResponseEntity<String> readReport() throws Exception {
    String outputPath = "/reports/fix_report.txt";

    java.nio.file.Path path = java.nio.file.Paths.get(outputPath);

    if (!java.nio.file.Files.exists(path)) {
        return ResponseEntity.status(404).body("Report not found. Generate it first.");
    }

    String content = java.nio.file.Files.readString(path);
    return ResponseEntity.ok(content);
}

}
