package org.personal.library.service.security;

import org.personal.library.util.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.nio.file.Path;

@Service
public class VirusScanService {

    @Value("${app.virus.scan.host:localhost}")
    private String clamavHost;

    @Value("${app.virus.scan.port:3310}")
    private int clamavPort;

    public void scanPdf(Path pdfPath) {
        if (pdfPath == null) {
            throw new AppException("Virus scan failed: missing PDF path", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            ClamavClient client = new ClamavClient(clamavHost, clamavPort);
            ScanResult result = client.scan(pdfPath);
            if (result instanceof ScanResult.VirusFound) {
                ScanResult.VirusFound virusFound = (ScanResult.VirusFound) result;
                throw new AppException("Virus detected in uploaded PDF: " + virusFound.getFoundViruses(), HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException("Unable to run virus scan: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
