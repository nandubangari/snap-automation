package com.snap.pages;


import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.*;

import java.net.MalformedURLException;
import java.util.*;

public class Snap {
    private AndroidDriver driver;
    private static final By chatButton = By.xpath("//android.view.ViewGroup[@content-desc=\"Chat\"]");
    private static final By newChatButton = By.xpath("//android.widget.RelativeLayout");
    private static final By sectionsBy = By.xpath("//android.view.View[matches(@resource-id, '^([A-Z]|#)$')]");
    //    private static By friendsSection = By.xpath("(/android.view.View)");
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


    public static int friendCount =0;
    public static int f =87;
    public void sendMessageToEveryOne() {
        utils.waitForSeconds(1);
        utils.hideKeyBoard();
        int sectionCount = utils.findElements(sectionsBy).size();
        System.out.println("Total Sections: "+sectionCount);
        for (int s = 1; s <= sectionCount; s++) {
            // get section by index
            By tempSection = By.xpath("(" + sectionsBy.toString().replace("By.xpath: ", "") + ")[" + s + "]");
            WebElement section = utils.findElement(tempSection);
//            friendCount = utils.findElements(section, friendsBy, tempSection).size();
            friendCount = section.findElements(friendsBy).size();
            System.out.println("Section " + s + " has " + friendCount + " friends");


            for (; f <= friendCount; f++) {
                utils.waitForSeconds(1);
                utils.hideKeyBoard();
                section = utils.findElement(tempSection);
                By tempFriend = By.xpath("(" + friendsBy.toString().replace("By.xpath: ", "") + ")[" + f + "]");
                WebElement friend = utils.findElement(section, tempFriend, tempSection);
                try {
                    String friendName = friend.findElement(friendNameBy).getText();
                    System.out.println(f+": " + friendName);
                    utils.scrollElementIntoViewSafe(friend, friendNameBy);
                    friend.findElement(friendNameBy).click();
                    sendMessageOrUnfriend(friendName);
                } catch (WebDriverException e) {
                    driver = Utils.repairDriver(driver, e);
                    e.printStackTrace();
                }
            }
            f=1;
        }
    }

//public static int friendCount =0;
//    public static int f =87;
//    public void sendMessageToEveryOne() {
//        utils.waitForSeconds(1);
//        utils.hideKeyBoard();
//        int sectionCount = utils.findElements(sectionsBy).size();
//        System.out.println("Total Sections: "+sectionCount);
//        for (int s = 1; s <= sectionCount; s++) {
//            // get section by index
//            By tempSection = By.xpath("(" + sectionsBy.toString().replace("By.xpath: ", "") + ")[" + s + "]");
//            WebElement section = utils.findElement(tempSection);
//            friendCount = utils.findElements(section, friendsBy, tempSection).size();
//            System.out.println("Section " + s + " has " + friendCount + " friends");
//            System.out.println(driver.findElement(se));
//
//            for (; f <= friendCount; f++) {
//                utils.waitForSeconds(1);
//                utils.hideKeyBoard();
//                section = utils.findElement(tempSection);
//                By tempFriend = By.xpath("(" + friendsBy.toString().replace("By.xpath: ", "") + ")[" + f + "]");
//                WebElement friend = utils.findElement(section, tempFriend, tempSection);
//                try {
//                    String friendName = friend.findElement(friendNameBy).getText();
//                    System.out.println(f+": " + friendName);
//                    utils.scrollElementIntoViewSafe(friend, friendNameBy);
//                    friend.click();
//                    sendMessageOrUnfriend(friendName);
//                } catch (WebDriverException e) {
//                    driver = Utils.repairDriver(driver, e);
//                }
//            }
//            f=1;
//        }
//    }


    public void sendMessageOrUnfriend(String name) {
        utils.hideKeyBoard();
        utils.clickElement(chatButtonI);
        utils.hideKeyBoard();
        utils.waitForSeconds(1);
        try {

            if (utils.isDisplayed(addFriendButton) || !utils.isDisplayed(videoCall)) {
                System.out.println("Not friend");
                if(!name.toLowerCase().contains("hyderabad")) {
                    unfriend();
                    friendCount--;
                    f--;
                }else{
                    utils.hideKeyBoard();
                    utils.waitForSeconds(1);
                    utils.navigateBack();
                    utils.waitForSeconds(1);
                    for(int i=0;i<3;i++){
                        if(utils.isDisplayed(newChatButton)){
                            break;
                        }
                        utils.navigateBack();
                    }
                    utils.clickElement(newChatButton);
                }
            } else {
                System.out.println("Friend");
                sendMessage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage();
        }

    }

    public void unfriend() {

        utils.clickElement(profile);
        utils.waitForSeconds(1);

        utils.clickElement(otherOptions);
        utils.waitForSeconds(1);
        if(utils.isDisplayed(mangeFriendShip)){
            utils.clickElement(mangeFriendShip);
            utils.waitForSeconds(1);
            utils.clickElement(removeFirend);
            utils.waitForSeconds(1);
            utils.clickElement(remove);
            utils.waitUntilElementDisappears(remove,15);
        }
        if(utils.isDisplayed(removeSwitch)){
            utils.clickElement(removeSwitch);
            utils.waitForSeconds(1);
            utils.clickElement(yes);
            utils.waitUntilElementDisappears(yes,15);
        }
        utils.waitForSeconds(1);
        utils.navigateBack();
        utils.waitForSeconds(1);
        utils.hideKeyBoard();
        utils.waitForSeconds(1);
        utils.navigateBack();
        utils.waitForSeconds(1);
        for(int i=0;i<3;i++){
            if(utils.isDisplayed(newChatButton)){
                break;
            }
            utils.navigateBack();
        }
        utils.clickElement(newChatButton);
    }

    public void sendMessage() {
        utils.setText(messageField, "Hii, Hloooo, Good morning.");
        utils.pressEnter();
        utils.setText(messageField, "Eala vunnav em chestunnav?");
        utils.pressEnter();
        utils.navigateBack();
        utils.navigateBack();
        utils.clickElement(newChatButton);
    }


}
