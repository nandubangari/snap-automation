package com.snap.pages;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class Utils {
    public AndroidDriver driver;

    public Utils(AndroidDriver driver) {
        this.driver = driver;
    }

    public void scrollElementIntoViewSafe(WebElement element, By innerElement) {
        try {
            Dimension screenSize = getWindowSize();
            int screenWidth = screenSize.getWidth();
            int screenHeight = screenSize.getHeight();
            System.out.println("Screen width: " + screenWidth);
            System.out.println("Screen height: " + screenHeight);
            int topSafeZone = 500;    // 500 px from top
            int bottomSafeZone = 100; // 200 px from bottom
            int maxRetries = 20;      // prevent infinite scroll

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                boolean elementVisible = false;
                int elementCenterY = screenHeight / 2; // default center
                try {
                    // Parse bounds attribute
                    String bounds = element.findElement(innerElement).getAttribute("bounds"); // format: [x1,y1][x2,y2]
                    System.out.println("Element bounds attribute: " + bounds);
                    assert bounds != null;
                    bounds = bounds.replace("[", "").replace("]", ",");
                    String[] parts = bounds.replace("[", "").replace("]", "").split(",");
                    int x1 = Integer.parseInt(parts[0]);
                    int y1 = Integer.parseInt(parts[1]);
                    int x2 = Integer.parseInt(parts[2]);
                    int y2 = Integer.parseInt(parts[3]);

                    elementCenterY = (y1 + y2) / 2;

                    // Check if element is within safe vertical and horizontal bounds
                    boolean visibleVertically = (y1 >= topSafeZone) && (y2 <= screenHeight - bottomSafeZone);
                    boolean visibleHorizontally = (x1 >= 0) && (x2 <= screenWidth);

                    elementVisible = visibleVertically && visibleHorizontally;

                    // Logging for debugging
                    System.out.println("Element top-left: (" + x1 + "," + y1 + "), bottom-right: (" + x2 + "," + y2 + ")");
                    System.out.println("Visible vertically: " + visibleVertically + ", horizontally: " + visibleHorizontally);

                    if (elementVisible) {
                        System.out.println("Element is in safe viewport, stopping scroll.");
                        break;
                    }


                    // Adaptive scroll distance
                    int distanceFromCenter = elementCenterY - (screenHeight / 2);
                    int scrollDistance = Math.min(Math.abs(distanceFromCenter), 300); // max 300
                    scrollDistance = Math.max(scrollDistance, 50); // minimum 50

                    boolean scrollUp = distanceFromCenter > 0;

                    int startX = screenWidth / 2;
                    int startY = screenHeight / 2;
                    int endY = scrollUp ? startY - scrollDistance : startY + scrollDistance;

                    // Perform swipe
                    final var finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                    var swipe = new Sequence(finger, 1);
                    swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
                    swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                    swipe.addAction(finger.createPointerMove(Duration.ofMillis(150), PointerInput.Origin.viewport(), startX, endY));
                    swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

                    try {
                        driver.perform(List.of(swipe));
                    } catch (WebDriverException e) {
                        driver = repairDriver(driver, e);
                        driver.perform(List.of(swipe));
                    }

                    // Pause for UI stabilization
                    Thread.sleep(2000);
                } catch (Exception ex) {
                    System.out.println("Element bounds not ready, will swipe anyway: " + ex.getMessage());
                }
            }


        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public Dimension getWindowSize() {
        try {
            return driver.manage().window().getSize();
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            return driver.manage().window().getSize();
        }
    }

    private static UiAutomator2Options getUiAutomator2Options() {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");

        // Snapchat package & activity
        options.setAppPackage("com.snapchat.android");
        options.setAppActivity("com.snap.mushroom.MainActivity");
        options.setNewCommandTimeout(Duration.ofSeconds(600));
        options.setCapability("appium:autoGrantPermissions", true);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);

        // Stability configs
        options.setNoReset(true);
        options.setFullReset(false);
        return options;
    }

    private static UiAutomator2Options getRepairUiAutomator2Options() {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setNewCommandTimeout(Duration.ofSeconds(600));
        options.setCapability("appium:autoGrantPermissions", true);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);

        // Stability configs
        options.setNoReset(true);
        options.setFullReset(false);
        return options;
    }

    public static AndroidDriver createDriver() throws MalformedURLException {
        return new AndroidDriver(
                new URL("http://127.0.0.1:4723/"),
                getUiAutomator2Options()
        );
    }

    public static AndroidDriver repairDriver() throws MalformedURLException {
        return new AndroidDriver(
                new URL("http://127.0.0.1:4723/"),
                getRepairUiAutomator2Options()
        );
    }

    /**
     * Repair method – if driver is null, quit, or socket dropped,
     * this will reinitialize the driver using the same options.
     */
    public static AndroidDriver repairDriver(AndroidDriver driver, Exception e) {


        System.out.println("⚠️ Driver session broken: " + e.getMessage());
        try {
            driver = repairDriver();
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Failed to repair driver", ex);
        }
        System.out.println("🔄 Driver repaired successfully!");

        return driver;
    }

    public void clickElement(By by) {
        try {
            driver.findElement(by).click();
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            driver.findElement(by).click();
        }
    }

    public void setText(By by, String text) {
        try {
            driver.findElement(by).sendKeys(text);
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            driver.findElement(by).sendKeys(text);
        }
    }

    public void hideKeyBoard() {
        try {
            driver.hideKeyboard();
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            driver.hideKeyboard();
        }
    }

    public void navigateBack() {
        try {
            driver.navigate().back();
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            driver.navigate().back();
        }
    }

    public void pressEnter() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
        }
    }

    public void waitForSeconds(int n) {
        try {
            Thread.sleep(n * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<WebElement> findElements(By by) {
        try {
            return driver.findElements(by);
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            return driver.findElements(by);
        }
    }

    public WebElement findElement(By by) {
        try {
            return driver.findElement(by);
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            return driver.findElement(by);
        }
    }

    public WebElement findElement(WebElement element, By by, By parent) {
        try {
            return element.findElement(by);
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            element = driver.findElement(parent);
            return element.findElement(by);
        }
    }

    public List<WebElement> findElements(WebElement element, By by, By parent) {
        try {
            return element.findElements(by);
        } catch (WebDriverException e) {
            driver = repairDriver(driver, e);
            element = driver.findElement(parent);
            return element.findElements(by);
        }
    }


}
