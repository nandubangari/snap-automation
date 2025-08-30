package com.snap;

import com.utils.Utils;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Snap {
    private static final Logger log = LoggerFactory.getLogger(Snap.class);

    private AndroidDriver driver;
    private Utils utils;

    // XPaths (kept as you provided)
    private static final By chatButton = By.xpath("//android.view.ViewGroup[@content-desc=\"Chat\"]");
    private static final By newChatButton = By.xpath("//android.widget.RelativeLayout");
    private static final By sectionsBy = By.xpath("//android.view.View[matches(@resource-id, '^([A-Z]|#)$')]");
    private static final By allViewsBy = By.xpath("//android.view.View"); // fallback
    private static final By friendsBy = By.xpath("//android.view.View[starts-with(@resource-id,'select_recipients_friends')]");
    private static final By friendNameBy = By.xpath(".//android.widget.TextView[1]");
    private static final By chatButtonI = By.xpath("//android.widget.Button[@content-desc=\"Chat\"]");
    private static final By messageField = By.xpath("//android.widget.EditText[@resource-id=\"com.snapchat.android:id/chat_input_text_field\"]");

    private static final By addFriendButton = By.xpath("//android.widget.Button[@resource-id=\"chat-header-add-friend-button\"]");
    private static final By profile = By.xpath("//android.widget.TextView[@resource-id=\"com.snapchat.android:id/0_resource_name_obfuscated\"]");
    private static final By otherOptions = By.xpath("(//android.view.View[@resource-id=\"upp-menu-button\"]) | ((//android.view.View[@resource-id=\"unified_profile_icon_button_action_menu\"])[1])");
    private static final By removeSwitch = By.xpath("//android.widget.TextView[@content-desc=\"Add\"]");
    private static final By yes = By.xpath("//android.view.View[@resource-id=\"com.snapchat.android:id/composer_alert_confirm\"]");
    private static final By mangeFriendShip = By.xpath("//javaClass[@text=\"Manage Friendship\"]");
    private static final By removeFirend = By.xpath("//javaClass[@text=\"Remove Friend\"]");
    private static final By remove = By.xpath("//android.widget.LinearLayout[@resource-id=\"com.snapchat.android:id/dialog_content\"]/android.view.View");
    private static final By videoCall = By.xpath("//android.view.View[@content-desc=\"Start a video call\"]");

    // Pattern to extract single-char id (fallback)
    private static final Pattern SECTION_ID_PATTERN = Pattern.compile("^([A-Z]|#)$");

    public Snap() throws MalformedURLException {
        this.driver = Utils.createDriver();
        this.utils = new Utils(this.driver);
        log.info("✅ Snapchat launched successfully!");
    }

    public void clickOnChat() {
        utils.clickElement(chatButton);
    }

    public void clickOnNewChatButton() {
        utils.clickElement(newChatButton);
    }

    /**
     * Primary: get sections via provided XPath. Fallback: get all android.view.View and filter
     * resource-id in Java (handles cases where matches() isn't supported by the XPath engine).
     */
    private List<WebElement> getSectionsSafe() {
        List<WebElement> sections = new ArrayList<>();
        try {
            sections = utils.findElements(sectionsBy);
            if (sections == null) sections = new ArrayList<>();
            if (!sections.isEmpty()) {
                log.debug("Found {} sections using sectionsBy xpath", sections.size());
                return sections;
            }
        } catch (Exception e) {
            log.debug("sectionsBy xpath failed/returned nothing: {}", e.getMessage());
        }

        // Fallback approach
        log.debug("Falling back to scanning all views and filtering resource-id A..Z / #");
        try {
            List<WebElement> all = utils.findElements(allViewsBy);
            for (WebElement v : all) {
                try {
                    String rid = v.getAttribute("resource-id");
                    if (rid == null || rid.isBlank()) continue;
                    // If resource-id contains a colon or slash or package prefix, pick last token
                    String last = rid.contains("/") ? rid.substring(rid.lastIndexOf("/") + 1) : rid;
                    last = last.contains(":") ? last.substring(last.lastIndexOf(":") + 1) : last;
                    if (SECTION_ID_PATTERN.matcher(last).matches()) {
                        sections.add(v);
                    }
                } catch (StaleElementReferenceException see) {
                    log.debug("stale element while filtering sections; skipping one");
                }
            }
        } catch (WebDriverException e) {
            log.warn("Error while gathering fallback sections: {}. Attempting driver repair.", e.getMessage());
            this.driver = Utils.repairDriverSafely(this.driver, e);
            this.utils = new Utils(this.driver);
            // one more try after repair
            try {
                List<WebElement> all = utils.findElements(allViewsBy);
                for (WebElement v : all) {
                    try {
                        String rid = v.getAttribute("resource-id");
                        if (rid == null || rid.isBlank()) continue;
                        String last = rid.contains("/") ? rid.substring(rid.lastIndexOf("/") + 1) : rid;
                        last = last.contains(":") ? last.substring(last.lastIndexOf(":") + 1) : last;
                        if (SECTION_ID_PATTERN.matcher(last).matches()) {
                            sections.add(v);
                        }
                    } catch (StaleElementReferenceException ignored) { }
                }
            } catch (Exception ex) {
                log.error("Fallback attempt after repair also failed: {}", ex.getMessage(), ex);
            }
        }
        log.debug("Sections found via fallback: {}", sections.size());
        return sections;
    }

    /**
     * Iterate sections A..Z/# and inside each, iterate friends list snapshot.
     * Uses fresh lookups each iteration to reduce staleness and uses repair on WebDriverException.
     */
    public void sendMessageToEveryOne() {
        utils.waitForSeconds(1);
        utils.hideKeyBoard();

        List<WebElement> sections = getSectionsSafe();
        log.info("Total Sections: {}", sections.size());

        for (int s = 0; s < sections.size(); s++) {
            log.info("Processing section {}/{}", s + 1, sections.size());

            // Re-acquire the section element fresh to avoid stale refs
            try {
                List<WebElement> freshSections = getSectionsSafe();
                if (s >= freshSections.size()) {
                    log.warn("Section index {} no longer present; skipping", s);
                    continue;
                }
                WebElement section = freshSections.get(s);

                List<WebElement> friends = new ArrayList<>();
                try {
                    friends = section.findElements(friendsBy);
                } catch (StaleElementReferenceException e) {
                    log.debug("Section stale when fetching friends; re-finding section & friends");
                    freshSections = getSectionsSafe();
                    if (s >= freshSections.size()) {
                        log.warn("Section disappeared after stale; skipping");
                        continue;
                    }
                    section = freshSections.get(s);
                    friends = section.findElements(friendsBy);
                } catch (WebDriverException e) {
                    log.warn("Error fetching friends: {}. Repairing driver and retrying.", e.getMessage());
                    this.driver = Utils.repairDriverSafely(this.driver, e);
                    this.utils = new Utils(this.driver);
                    freshSections = getSectionsSafe();
                    if (s >= freshSections.size()) {
                        log.warn("Section not found after repair; skipping");
                        continue;
                    }
                    section = freshSections.get(s);
                    friends = section.findElements(friendsBy);
                }

                log.info("Section {} has {} friends (snapshot)", s + 1, friends.size());

                for (int fIndex = 0; fIndex < friends.size(); fIndex++) {
                    utils.waitForSeconds(1);
                    utils.hideKeyBoard();

                    // Re-fetch section & friends each loop to avoid using stale objects
                    try {
                        List<WebElement> freshSections2 = getSectionsSafe();
                        if (s >= freshSections2.size()) {
                            log.warn("Section disappeared mid-iteration; breaking friend loop");
                            break;
                        }
                        section = freshSections2.get(s);
                        friends = section.findElements(friendsBy);
                        if (fIndex >= friends.size()) {
                            log.warn("Friend index {} out of bounds after re-fetch; breaking.", fIndex);
                            break;
                        }
                    } catch (WebDriverException e) {
                        log.warn("Error re-fetching section/friends: {}. Repairing driver.", e.getMessage());
                        this.driver = Utils.repairDriverSafely(this.driver, e);
                        this.utils = new Utils(this.driver);
                        continue; // try next iteration
                    }

                    WebElement friend = friends.get(fIndex);
                    String friendName;
                    try {
                        friendName = friend.findElement(friendNameBy).getText();
                    } catch (StaleElementReferenceException | NoSuchElementException ex) {
                        log.debug("Friend element stale when reading name; re-finding friend element");
                        friends = section.findElements(friendsBy);
                        if (fIndex >= friends.size()) {
                            log.warn("Friend not present after re-find; skipping index {}", fIndex);
                            continue;
                        }
                        friend = friends.get(fIndex);
                        try {
                            friendName = friend.findElement(friendNameBy).getText();
                        } catch (Exception exc) {
                            log.warn("Couldn't read friend name, skipping friend at index {}: {}", fIndex, exc.getMessage());
                            continue;
                        }
                    }

                    log.info("Processing friend {} (section {}, index {})", friendName, s + 1, fIndex + 1);

                    // Scroll into view and click
                    try {
                        utils.scrollElementIntoViewSafe(friend, friendNameBy);
                    } catch (Exception ex) {
                        log.debug("scrollElementIntoViewSafe threw: {}", ex.getMessage());
                    }

                    try {
                        WebElement nameEl = friend.findElement(friendNameBy);
                        nameEl.click();
                    } catch (WebDriverException clickEx) {
                        log.warn("Failed clicking friend name: {}. Attempting repair and continuing.", clickEx.getMessage());
                        this.driver = Utils.repairDriverSafely(this.driver, clickEx);
                        this.utils = new Utils(this.driver);
                        safeEnsureNewChatScreen();
                        continue;
                    }

                    // After opening chat, decide to send or unfriend
                    try {
                        sendMessageOrUnfriend(friendName);
                    } catch (WebDriverException e) {
                        log.error("Error during send/unfriend for {}: {}. Repairing driver.", friendName, e.getMessage());
                        this.driver = Utils.repairDriverSafely(this.driver, e);
                        this.utils = new Utils(this.driver);
                    }
                } // end friends loop
            } catch (Exception outerEx) {
                log.error("Unexpected error processing section {}: {}", s + 1, outerEx.getMessage(), outerEx);
                this.driver = Utils.repairDriverSafely(this.driver, outerEx instanceof WebDriverException ? (WebDriverException) outerEx : new WebDriverException(outerEx));
                this.utils = new Utils(this.driver);
            }
        } // end sections loop

        log.info("Finished sendMessageToEveryOne()");
    }

    private void safeEnsureNewChatScreen() {
        try {
            int tries = 0;
            while (tries++ < 4 && !utils.isDisplayed(newChatButton)) {
                utils.navigateBack();
                utils.waitForSeconds(1);
            }
            if (utils.isDisplayed(newChatButton)) {
                utils.clickElement(newChatButton);
            } else {
                log.warn("Could not ensure new chat screen (newChatButton not visible).");
            }
        } catch (WebDriverException e) {
            log.warn("safeEnsureNewChatScreen encountered: {}. Repairing driver.", e.getMessage());
            this.driver = Utils.repairDriverSafely(this.driver, e);
            this.utils = new Utils(this.driver);
        }
    }

    public void sendMessageOrUnfriend(String name) {
        utils.hideKeyBoard();
        utils.clickElement(chatButtonI);
        utils.hideKeyBoard();
        utils.waitForSeconds(1);
        try {
            boolean addFriendVisible = utils.isDisplayed(addFriendButton);
            boolean videoCallVisible = utils.isDisplayed(videoCall);

            if (addFriendVisible || !videoCallVisible) {
                log.info("Not friend: {}", name);
                if (!name.toLowerCase().contains("hyderabad")) {
                    unfriend();
                } else {
                    safeReturnToNewChat();
                }
            } else {
                log.info("Friend - sending message to {}", name);
                sendMessage();
            }
        } catch (Exception e) {
            log.warn("Exception in sendMessageOrUnfriend: {} — attempting sendMessage fallback", e.getMessage());
            try {
                sendMessage();
            } catch (Exception ex) {
                log.error("Fallback sendMessage also failed: {}", ex.getMessage(), ex);
            }
        }
    }

    private void safeReturnToNewChat() {
        try {
            utils.hideKeyBoard();
            utils.waitForSeconds(1);
            utils.navigateBack();
            utils.waitForSeconds(1);
            for (int i = 0; i < 3; i++) {
                if (utils.isDisplayed(newChatButton)) break;
                utils.navigateBack();
                utils.waitForSeconds(1);
            }
            if (utils.isDisplayed(newChatButton)) utils.clickElement(newChatButton);
            else log.warn("Could not return to new chat screen.");
        } catch (WebDriverException e) {
            log.warn("safeReturnToNewChat failed: {}. Repairing driver.", e.getMessage());
            this.driver = Utils.repairDriverSafely(this.driver, e);
            this.utils = new Utils(this.driver);
        }
    }

    public void unfriend() {
        try {
            utils.clickElement(profile);
            utils.waitForSeconds(1);

            utils.clickElement(otherOptions);
            utils.waitForSeconds(1);
            if (utils.isDisplayed(mangeFriendShip)) {
                utils.clickElement(mangeFriendShip);
                utils.waitForSeconds(1);
                utils.clickElement(removeFirend);
                utils.waitForSeconds(1);
                utils.clickElement(remove);
                utils.waitUntilElementDisappears(remove, 15);
            }
            if (utils.isDisplayed(removeSwitch)) {
                utils.clickElement(removeSwitch);
                utils.waitForSeconds(1);
                utils.clickElement(yes);
                utils.waitUntilElementDisappears(yes, 15);
            }

            utils.waitForSeconds(1);
            utils.navigateBack();
            utils.waitForSeconds(1);
            utils.hideKeyBoard();
            utils.waitForSeconds(1);
            utils.navigateBack();
            utils.waitForSeconds(1);
            for (int i = 0; i < 3; i++) {
                if (utils.isDisplayed(newChatButton)) break;
                utils.navigateBack();
                utils.waitForSeconds(1);
            }
            utils.clickElement(newChatButton);
        } catch (WebDriverException e) {
            log.error("unfriend encountered: {}. Repairing driver.", e.getMessage());
            this.driver = Utils.repairDriverSafely(this.driver, e);
            this.utils = new Utils(this.driver);
        }
    }

    public void sendMessage() {
        try {
            utils.setText(messageField, "Hii, Hloooo, Good morning.");
            utils.pressEnter();
            utils.setText(messageField, "Eala vunnav em chestunnav?");
            utils.pressEnter();
            utils.navigateBack();
            utils.navigateBack();
            utils.clickElement(newChatButton);
        } catch (WebDriverException e) {
            log.error("sendMessage failed: {}. Repairing driver.", e.getMessage());
            this.driver = Utils.repairDriverSafely(this.driver, e);
            this.utils = new Utils(this.driver);
        }
    }

    /**
     * Close driver cleanly so Main can call snap.close() instead of reflection.
     */
    public void close() {
        try {
            if (driver != null) {
                log.info("Quitting AndroidDriver session...");
                driver.quit();
            }
        } catch (WebDriverException e) {
            log.warn("Error quitting driver: {}", e.getMessage());
        } finally {
            driver = null;
        }
    }
}
