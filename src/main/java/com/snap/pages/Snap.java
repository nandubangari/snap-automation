package com.snap.pages;


import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.*;

import java.net.MalformedURLException;
import java.util.*;

public class Snap {
    private AndroidDriver driver;
    private static final By chatButton = By.xpath("//android.view.ViewGroup[@content-desc=\"Chat\"]");
    private static final By newChatButton = By.xpath("//android.widget.RelativeLayout");
    private static final By sectionsBy = By.xpath(
            "//android.view.View[string-length(normalize-space(@resource-id))=1]"
    );
    //    private static By friendsSection = By.xpath("(/android.view.View)");
    private static final By friendsBy = By.xpath("//android.view.View//(android.widget.TextView)[1]/..");
    private static final By friendNameBy = By.xpath(".//android.widget.TextView[1]");
    private static final By chatButtonI = By.xpath("//android.widget.Button[@content-desc=\"Chat\"]");
    private static final By messageField = By.xpath("//android.widget.EditText[@resource-id=\"com.snapchat.android:id/chat_input_text_field\"]");

    private static final By addFriendButton = By.id("chat-header-add-friend-button");
    private static final By profile = By.xpath("//android.widget.TextView[@resource-id=\"com.snapchat.android:id/0_resource_name_obfuscated\"]");
    private static final By otherOptions = By.xpath("(//android.view.View[@resource-id=\"unified_profile_icon_button_action_menu\"])[2]");
    private static final By mangeFriendShip = By.xpath("//javaClass[@text=\"Manage Friendship\"]");
    private static final By removeFirend = By.xpath("//javaClass[@text=\"Remove Friend\"]");
    private static final By remove = By.xpath("//android.widget.LinearLayout[@resource-id=\"com.snapchat.android:id/dialog_content\"]/android.view.View");
    private static Utils utils;

    public Snap() throws MalformedURLException {
        this.driver = Utils.createDriver();
        utils = new Utils(driver);
        System.out.println("✅ Snapchat launched successfully!");
    }

    public void clickOnChat() {
        utils.clickElement(chatButton);
    }

    public void clickOnNewChatButton() {
        utils.clickElement(newChatButton);
    }


    public void sendMessageToEveryOne() {
        utils.waitForSeconds(2);
        utils.hideKeyBoard();
        int sectionCount = utils.findElements(sectionsBy).size();
        for (int s = 1; s <= sectionCount; s++) {
            // get section by index
            By tempSection = By.xpath("(" + sectionsBy.toString().replace("By.xpath: ", "") + ")[" + s + "]");
            WebElement section = utils.findElement(tempSection);
            int friendCount = utils.findElements(section, friendsBy, tempSection).size();
            System.out.println("Section " + s + " has " + friendCount + " friends");
            for (int f = 1; f <= friendCount; f++) {
                utils.waitForSeconds(2);
                utils.hideKeyBoard();
                section = utils.findElement(tempSection);
                By tempFriend = By.xpath("(" + friendsBy.toString().replace("By.xpath: ", "") + ")[" + f + "]");
                WebElement friend = utils.findElement(section, tempFriend, tempSection);
                try {
                    String friendName = friend.findElement(friendNameBy).getText();
                    System.out.println("Friend: " + friendName);
                    utils.scrollElementIntoViewSafe(friend, friendNameBy);
                    friend.click();
                    sendMessageOrUnfriend();
                } catch (WebDriverException e) {
                    driver = Utils.repairDriver(driver,e);
                }
            }
        }
    }


    public void sendMessageOrUnfriend() {
        utils.hideKeyBoard();
        utils.clickElement(chatButtonI);
        try {

            if (utils.findElement(addFriendButton).isDisplayed()) {
                unfriend();
            } else {
                sendMessage();
            }
        } catch (Exception e) {
            sendMessage();
        }

    }

    public void unfriend() {
        utils.clickElement(profile);
        utils.clickElement(otherOptions);
        utils.clickElement(mangeFriendShip);
        utils.clickElement(removeFirend);
        utils.clickElement(remove);
     utils.navigateBack();
       utils.navigateBack();

        utils.clickElement(newChatButton);
    }

    public void sendMessage() {
        utils.setText(messageField,"Hii, Hloooo, Good morning.");
        utils.pressEnter();
        utils.setText(messageField,"Eala vunnav em chestunnav?");
        utils.pressEnter();
        utils.navigateBack();
        utils.navigateBack();
        utils.clickElement(newChatButton);
    }


}
