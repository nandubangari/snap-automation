package com.utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public AndroidDriver driver;

    // configurable timeouts
    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(7);
    private static final Duration ACTION_RETRY_INTERVAL = Duration.ofMillis(500);
    private static final int DRIVER_REPAIR_MAX_RETRIES = 2;

    public Utils(AndroidDriver driver) {
        this.driver = driver;
    }

    /**
     * Scrolls element into view using element bounds when available.
     * If bounds are not available it will use a safe directional swipe with retries.
     */
    public void scrollElementIntoViewSafe(WebElement element, By innerElement) {
        try {
            Dimension screenSize = getWindowSize();
            int screenWidth = screenSize.getWidth();
            int screenHeight = screenSize.getHeight();
            int topSafeZone = 500;    // px from top, tune as needed
            int bottomSafeZone = 200; // px from bottom
            int maxRetries = 20;      // prevent infinite scroll

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                boolean elementVisible = false;
                int elementCenterY = screenHeight / 2;

                try {
                    String bounds = element.findElement(innerElement).getAttribute("bounds");
                    if (bounds != null && !bounds.isBlank()) {
                        // parse [x1,y1][x2,y2]
                        String[] parts = bounds.replace("[", "").replace("]", ",").split(",");
                        int x1 = Integer.parseInt(parts[0].trim());
                        int y1 = Integer.parseInt(parts[1].trim());
                        int x2 = Integer.parseInt(parts[2].trim());
                        int y2 = Integer.parseInt(parts[3].trim());

                        elementCenterY = (y1 + y2) / 2;

                        boolean visibleVertically = (y1 >= topSafeZone) && (y2 <= screenHeight - bottomSafeZone);
                        boolean visibleHorizontally = (x1 >= 0) && (x2 <= screenWidth);

                        elementVisible = visibleVertically && visibleHorizontally;
                        log.debug("Bounds parsed: ({},{})-({},{}) | visibleVertically={} visibleHorizontally={}",
                                x1, y1, x2, y2, visibleVertically, visibleHorizontally);

                        if (elementVisible) {
                            log.info("Element in safe viewport after {} attempts", attempt);
                            return;
                        }
                    } else {
                        log.debug("Element bounds empty or null; will attempt heuristic swipe.");
                    }
                } catch (Exception ex) {
                    log.debug("Could not parse element bounds: {} — will attempt swipe. attempt={}", ex.getMessage(), attempt);
                }

                // Adaptive scroll distance
                int distanceFromCenter = elementCenterY - (screenHeight / 2);
                int scrollDistance = Math.min(Math.abs(distanceFromCenter), 300); // max 300 px
                scrollDistance = Math.max(scrollDistance, 120); // minimum 120 px for more reliable movement

                boolean scrollUp = distanceFromCenter > 0;
                int startX = screenWidth / 2;
                int startY = screenHeight / 2;
                int endY = scrollUp ? startY - scrollDistance : startY + scrollDistance;

                // Build swipe sequence
                final var finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                var swipe = new Sequence(finger, 1);
                swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
                swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(200), PointerInput.Origin.viewport(), startX, endY));
                swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

                // perform swipe with retry on session issues
                try {
                    performSafe(swipe);
                } catch (Exception e) {
                    log.warn("Swipe attempt failed: {}", e.getMessage());
                    driver = repairDriverSafely(driver, e);
                    performSafe(swipe);
                }

                // short wait for UI stabilization — prefer explicit wait for element presence after swipe
                smallWait();
            }
            log.warn("Reached max scroll attempts; element might not be visible.");
        } catch (Exception e) {
            log.error("scrollElementIntoViewSafe failed: {}", e.getMessage(), e);
        }
    }

    private void smallWait() {
        try {
            Thread.sleep(700); // small, predictable pause for UI to settle (use sparingly)
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public Dimension getWindowSize() {
        try {
            return driver.manage().window().getSize();
        } catch (WebDriverException e) {
            log.warn("getWindowSize raised WebDriverException: {} — attempting repair", e.getMessage());
            driver = repairDriverSafely(driver, e);
            return driver.manage().window().getSize();
        }
    }

    private static UiAutomator2Options getUiAutomator2Options() {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setAppPackage("com.snapchat.android");
        options.setAppActivity("com.snap.mushroom.MainActivity");
        options.setCapability("appium:autoGrantPermissions", true);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);

        // Correct values: 86400 seconds = 24 hours (was previously commented as 2 hours)
        options.setCapability("newCommandTimeout", 86400); // keep long, but comment is now correct (24 hours)
        options.setCapability("adbExecTimeout", 8640000); // large value to tolerate long adb ops
        options.setCapability("uiautomator2ServerLaunchTimeout", 8640000);
        options.setCapability("uiautomator2ServerInstallTimeout", 8640000);

        options.setNoReset(true);
        options.setFullReset(false);
        return options;
    }

    private static UiAutomator2Options getRepairUiAutomator2Options() {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setCapability("appium:autoGrantPermissions", true);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);

        options.setCapability("newCommandTimeout", 86400);
        options.setCapability("adbExecTimeout", 8640000);
        options.setCapability("uiautomator2ServerLaunchTimeout", 8640000);
        options.setCapability("uiautomator2ServerInstallTimeout", 8640000);

        options.setNoReset(true);
        options.setFullReset(false);
        return options;
    }

    public static AndroidDriver createDriver() throws MalformedURLException {
        log.info("Creating new AndroidDriver session");
        return new AndroidDriver(new URL("http://127.0.0.1:4723/"), getUiAutomator2Options());
    }

    public static AndroidDriver repairDriver() throws MalformedURLException {
        log.info("Repairing AndroidDriver by creating a fresh session");
        return new AndroidDriver(new URL("http://127.0.0.1:4723/"), getRepairUiAutomator2Options());
    }

    /**
     * Thread-safe driver repair wrapper. Attempts to re-create driver if it's null or session invalid.
     * Limits number of retries to avoid flapping.
     */
    public static synchronized AndroidDriver repairDriverSafely(AndroidDriver currentDriver, Exception e) {
        log.warn("⚠️ Driver session broken or threw: {}", e.toString());
        int attempts = 0;
        while (attempts < DRIVER_REPAIR_MAX_RETRIES) {
            attempts++;
            try {
                AndroidDriver newDriver = repairDriver();
                log.info("🔄 Driver repaired successfully on attempt {}", attempts);
                return newDriver;
            } catch (MalformedURLException ex) {
                throw new RuntimeException("Failed to repair driver due to MalformedURLException", ex);
            } catch (WebDriverException ex) {
                log.error("Attempt {} to repair driver failed: {}", attempts, ex.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new RuntimeException("Could not repair driver after retries; check Appium server and device state.");
    }

    /**
     * Safer perform() wrapper - retries on transient session issues.
     */
    private void performSafe(Sequence... sequences) {
        int tries = 0;
        while (true) {
            try {
                driver.perform(List.of(sequences));
                return;
            } catch (WebDriverException e) {
                tries++;
                log.debug("perform() raised WebDriverException (try {}): {}", tries, e.getMessage());
                // if it's a session-related exception, try repair once
                if (tries == 1) {
                    driver = repairDriverSafely(driver, e);
                    continue;
                }
                throw e;
            }
        }
    }

    public void clickElement(By by) {
        log.debug("clickElement -> {}", by);
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                waitUntilElementVisible(by, DEFAULT_WAIT);
                driver.findElement(by).click();
                return;
            } catch (ElementClickInterceptedException | StaleElementReferenceException ex) {
                log.warn("Attempt {} click intercepted/stale: {}. Retrying...", attempt, ex.getMessage());
                smallWait();
            } catch (WebDriverException e) {
                log.error("WebDriverException on clickElement: {}. Attempting repair.", e.getMessage());
                driver = repairDriverSafely(driver, e);
            }
        }
        throw new RuntimeException("clickElement failed after retries: " + by.toString());
    }

    public void setText(By by, String text) {
        log.debug("setText -> {} = {}", by, text);
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                waitUntilElementVisible(by, DEFAULT_WAIT);
                WebElement el = driver.findElement(by);
                el.clear();
                el.sendKeys(text);
                return;
            } catch (InvalidElementStateException | StaleElementReferenceException ex) {
                log.warn("Attempt {} setText failed: {}. Retrying...", attempt, ex.getMessage());
                smallWait();
            } catch (WebDriverException e) {
                log.error("WebDriverException on setText: {}. Attempting repair.", e.getMessage());
                driver = repairDriverSafely(driver, e);
            }
        }
        throw new RuntimeException("setText failed after retries: " + by.toString());
    }

    public void hideKeyBoard() {
        try {
            driver.hideKeyboard();
        } catch (WebDriverException e) {
            log.warn("hideKeyboard failed: {}. Trying repair.", e.getMessage());
            driver = repairDriverSafely(driver, e);
            driver.hideKeyboard();
        }
    }

    public void navigateBack() {
        try {
            driver.navigate().back();
        } catch (WebDriverException e) {
            log.warn("navigateBack failed: {}. Repairing driver.", e.getMessage());
            driver = repairDriverSafely(driver, e);
            driver.navigate().back();
        }
    }

    public void pressEnter() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
        } catch (WebDriverException e) {
            log.warn("pressEnter failed: {}. Repairing driver.", e.getMessage());
            driver = repairDriverSafely(driver, e);
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
        }
    }

    public void waitForSeconds(int n) {
        try {
            Thread.sleep(n * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<WebElement> findElements(By by) {
        try {
            waitUntilElementsPresent(by);
            return driver.findElements(by);
        } catch (WebDriverException e) {
            log.warn("findElements web driver exception: {}. Repairing.", e.getMessage());
            driver = repairDriverSafely(driver, e);
            waitUntilElementsPresent(by);
            return driver.findElements(by);
        }
    }

    public WebElement findElement(By by) {
        try {
            waitUntilElementVisible(by, DEFAULT_WAIT);
            return driver.findElement(by);
        } catch (WebDriverException e) {
            log.warn("findElement error: {}. Repairing.", e.getMessage());
            driver = repairDriverSafely(driver, e);
            waitUntilElementVisible(by, DEFAULT_WAIT);
            return driver.findElement(by);
        }
    }

    public WebElement findElement(WebElement element, By by, By parent) {
        try {
            waitUntilElementVisible(by, DEFAULT_WAIT);
            return element.findElement(by);
        } catch (WebDriverException e) {
            log.warn("findElement within parent failed: {}. Repairing driver & re-finding parent.", e.getMessage());
            driver = repairDriverSafely(driver, e);
            waitUntilElementVisible(parent, DEFAULT_WAIT);
            element = driver.findElement(parent);
            waitUntilElementVisible(by, DEFAULT_WAIT);
            return element.findElement(by);
        }
    }

    public List<WebElement> findElements(WebElement element, By by, By parent) {
        try {
            waitUntilElementsPresent(by);
            return element.findElements(by);
        } catch (WebDriverException e) {
            log.warn("findElements within parent failed: {}. Repairing driver & re-finding parent.", e.getMessage());
            driver = repairDriverSafely(driver, e);
            waitUntilElementVisible(parent, DEFAULT_WAIT);
            element = driver.findElement(parent);
            waitUntilElementsPresent(by);
            return element.findElements(by);
        }
    }

    public Boolean isDisplayed(By by) {
        try {
            return driver.findElement(by).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait until element disappears (invisible or removed from DOM)
     */
    public void waitUntilElementDisappears(By locator, int timeoutInSeconds) {
        try {
            Wait<AndroidDriver> wait = new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(timeoutInSeconds))
                    .pollingEvery(ACTION_RETRY_INTERVAL)
                    .ignoring(NoSuchElementException.class);

            wait.until((Function<AndroidDriver, Boolean>) d -> {
                try {
                    List<WebElement> els = d.findElements(locator);
                    return els.isEmpty() || els.stream().noneMatch(WebElement::isDisplayed);
                } catch (Exception ex) {
                    return true;
                }
            });
        } catch (TimeoutException e) {
            log.warn("waitUntilElementDisappears timed out for {}: {}", locator, e.getMessage());
        }
    }

    public void waitUntilElementPresent(By locator) {
        waitUntilElementVisible(locator, DEFAULT_WAIT);
    }

    public void waitUntilElementsPresent(By locator) {
        try {
            Wait<AndroidDriver> wait = new FluentWait<>(driver)
                    .withTimeout(DEFAULT_WAIT)
                    .pollingEvery(ACTION_RETRY_INTERVAL)
                    .ignoring(NoSuchElementException.class);

            wait.until(d -> !d.findElements(locator).isEmpty());
        } catch (TimeoutException e) {
            log.warn("waitUntilElementsPresent timed out for {}: {}", locator, e.getMessage());
        }
    }

    public void waitUntilElementVisible(By locator, Duration timeout) {
        try {
            FluentWait<AndroidDriver> wait = new FluentWait<>(driver)
                    .withTimeout(timeout)
                    .pollingEvery(ACTION_RETRY_INTERVAL).ignoring(NoSuchElementException.class,StaleElementReferenceException.class);

            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            log.debug("waitUntilElementVisible timed out for {} after {}: {}",
                    locator, timeout.toSeconds(), e.getMessage());
        }
    }
}
