package org.personal.library.service.security;

import org.personal.library.util.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.InputStream;
import java.nio.file.Path;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirusScanService {

    private final org.personal.library.config.AppProperties appProperties;

    /**
     * Scan pdf.
     *
     * @param pdfPath the pdfPath
     */
    public void scanPdf(Path pdfPath) {
        if (!appProperties.getVirusScan().isEnabled()) {
            log.info("Virus scan is disabled. Skipping scan for: {}", pdfPath);
            return;
        }

        if (pdfPath == null) {
            throw new AppException("Virus scan failed: missing PDF path", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            ClamavClient client = new ClamavClient(appProperties.getVirusScan().getHost(), appProperties.getVirusScan().getPort());
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
