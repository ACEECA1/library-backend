package org.personal.library.service.security;

import org.personal.library.util.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class VirusScanService {

    @Value("${app.virus.scan.command:clamscan --no-summary}")
    private String virusScanCommand;

    public void scanPdf(Path pdfPath) {
        if (pdfPath == null) {
            throw new AppException("Virus scan failed: missing PDF path", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String commandConfig = virusScanCommand != null ? virusScanCommand.trim() : "";
        if (commandConfig.isBlank()) {
            throw new AppException("Virus scan command is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<String> command = new ArrayList<>(Arrays.asList(commandConfig.split("\\s+")));
        command.add(pdfPath.toString());

        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new AppException("Unable to start virus scan: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String output;
        int exitCode;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            exitCode = process.waitFor();
        } catch (IOException e) {
            throw new AppException("Virus scan failed to read output: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Virus scan interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (exitCode == 0) {
            return;
        }

        String messageSuffix = output.isBlank() ? "" : " - " + output;
        if (exitCode == 1) {
            throw new AppException("Virus detected in uploaded PDF" + messageSuffix, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        throw new AppException("Virus scan failed with exit code " + exitCode + messageSuffix,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
