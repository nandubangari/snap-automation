package com.app;

import com.snap.pages.Snap;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.MalformedURLException;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Snap snap = null;
        try {
            log.info("Starting Snap automation...");
            snap = new Snap();

            // Main flow
            snap.clickOnChat();
            snap.clickOnNewChatButton();
            snap.sendMessageToEveryOne();

            log.info("Snap automation completed.");
        } catch (MalformedURLException e) {
            log.error("Failed to initialize Snap (MalformedURLException): {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during run: {}", e.getMessage(), e);
        } finally {
            // Attempt to gracefully quit the underlying AndroidDriver (uses reflection since driver is private)
            if (snap != null) {
                try {
                    Field driverField = Snap.class.getDeclaredField("driver");
                    driverField.setAccessible(true);
                    Object driverObj = driverField.get(snap);
                    if (driverObj instanceof AndroidDriver) {
                        AndroidDriver driver = (AndroidDriver) driverObj;
                        try {
                            log.info("Quitting AndroidDriver session...");
                            driver.quit();
                            log.info("AndroidDriver quit successfully.");
                        } catch (WebDriverException wde) {
                            log.warn("Error while quitting driver: {}", wde.getMessage(), wde);
                        }
                    } else {
                        log.warn("Driver field is null or not an AndroidDriver instance.");
                    }
                } catch (NoSuchFieldException nsfe) {
                    log.warn("Could not find driver field on Snap class: {}", nsfe.getMessage());
                } catch (IllegalAccessException iae) {
                    log.warn("Could not access driver field: {}", iae.getMessage());
                } catch (Exception ex) {
                    log.warn("Unexpected error while trying to quit driver: {}", ex.getMessage(), ex);
                }
            }
            log.info("Exiting process.");
            System.exit(0);
        }
    }
}
