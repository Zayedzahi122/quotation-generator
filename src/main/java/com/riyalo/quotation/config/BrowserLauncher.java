package com.riyalo.quotation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

/**
 * Opens the app in the default browser after startup. Enabled only in the packaged
 * application (quotation.auto-open-browser=true), never during development or tests.
 */
@Component
public class BrowserLauncher implements ApplicationRunner {

    private final boolean autoOpen;
    private final int port;

    public BrowserLauncher(@Value("${quotation.auto-open-browser:false}") boolean autoOpen,
                           @Value("${server.port:1120}") int port) {
        this.autoOpen = autoOpen;
        this.port = port;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoOpen) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("http://localhost:" + port));
            }
        } catch (Exception ignored) {
            // Opening a browser is a convenience only; never block startup on it.
        }
    }
}