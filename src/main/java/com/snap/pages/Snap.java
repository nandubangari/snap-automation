package com.snap.pages;


import com.google.common.collect.ImmutableMap;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.pagefactory.AppiumElementLocatorFactory;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.lang.model.element.Element;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.*;

public class Snap {
    private AndroidDriver driver;
    private static By chatButton = By.xpath("//android.view.ViewGroup[@content-desc=\"Chat\"]");
    private static By newChatButton = By.xpath("//android.widget.RelativeLayout");
    private static By sectionsBy = By.xpath(
            "//android.view.View[string-length(normalize-space(@resource-id))=1]"
    );
    //    private static By friendsSection = By.xpath("(/android.view.View)");
    private static By friendsBy = By.xpath("//android.view.View//(android.widget.TextView)[1]");
    private static By chatButtonI = By.xpath("//android.widget.Button[@content-desc=\"Chat\"]");
    private static By messageField = By.xpath("//android.widget.EditText[@resource-id=\"com.snapchat.android:id/chat_input_text_field\"]");

    public Snap() throws MalformedURLException {
        UiAutomator2Options options = getUiAutomator2Options();
        this.driver = new AndroidDriver(
                URI.create("http://127.0.0.1:4723/").toURL(),
                options
        );
        System.out.println("✅ Snapchat launched successfully!");
    }

    public void printCurrentActivity() {
        String currentActivity = driver.currentActivity();
        System.out.println("Current activity: " + currentActivity);
    }

    private static UiAutomator2Options getUiAutomator2Options() {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setDeviceName("Android Emulator"); // or your device ID
        options.setPlatformVersion("14");          // match your device Android version
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

    public void clickOnChat() {
        driver.findElement(chatButton).click();
    }

    public void clickOnNewChatButton() {
        driver.findElement(newChatButton).click();
    }

    public void closeKeyBoard() {
        driver.hideKeyboard();
    }

    public void sendMessageToEveryOne() {
        List<WebElement> sections = driver.findElements(sectionsBy);
        for (WebElement section : sections) {
            List<WebElement> friends = section.findElements(friendsBy);
            System.out.println("Section: " + section.getAttribute("resource-id"));
            System.out.println("No: " + friends.size());
            for (WebElement friend : friends) {

                String friendName = friend.getText();
                By location = AppiumBy.accessibilityId(friendName);
                System.out.println("Friend: " + friendName);

                System.out.println("Bound Location: " + friend.getAttribute("bounds"));
                System.out.println("Id: " + ((RemoteWebElement) friend).getId());
                System.out.println("Is element displayed: " + friend.isDisplayed());

                scrollElementIntoViewSafe(friend);


                friend.click();

            }


        }
    }


    public void sendMessageOrUnfriend() {
        driver.hideKeyboard();
        driver.findElement(messageField).sendKeys("Hii hloo");
        driver.pressKey(new KeyEvent(AndroidKey.ENTER));

    }


    public void scrollElementIntoViewSafe(WebElement element) {
        try {
            Dimension screenSize = driver.manage().window().getSize();
            int screenWidth = screenSize.getWidth();
            int screenHeight = screenSize.getHeight();
            System.out.println("Screen width: " + screenWidth);
            System.out.println("Screen height: " + screenHeight);

            int topSafeZone = 500;    // 500 px from top
            int bottomSafeZone = 200; // 200 px from bottom
            int maxRetries = 20;      // prevent infinite scroll
            int elementHeight = element.getSize().getHeight();
            for (int attempt = 0; attempt < maxRetries; attempt++) {

                boolean elementVisible = false;
                int elementCenterY = screenHeight / 2; // default

                try {
                    Point location = element.getLocation();
                    int elementY = location.getY();

                    int y1 = elementY;
                    int y2 = elementY + elementHeight;

                    elementCenterY = y1 + elementHeight / 2;

                    // Check if element is within safe vertical and horizontal bounds
                    boolean visibleVertically = (y1 >= topSafeZone) && (y2 <= screenHeight - bottomSafeZone);
                    boolean visibleHorizontally = (location.getX() >= 0) && ((location.getX() + element.getSize().getWidth()) <= screenWidth);

                    elementVisible = visibleVertically && visibleHorizontally;

                    // Logging for debugging
                    System.out.println("Eleemet current location: "+ y1);
//                    System.out.println("Element: " + element.getText());
                    System.out.println("Bounds: [" + y1 + "," + y2 + "]");
                    System.out.println("Visible vertically: " + visibleVertically + ", horizontally: " + visibleHorizontally);

                    if (elementVisible) {
                        System.out.println("Element is in safe viewport, stopping scroll.");
                        break;
                    }

                } catch (Exception ex) {
                    System.out.println("Element location not ready, will swipe anyway: " + ex.getMessage());
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

                driver.perform(Arrays.asList(swipe));

                // Pause for UI stabilization
                Thread.sleep(8000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
