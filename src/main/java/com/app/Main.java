package com.app;

import com.snap.pages.Snap;

import java.net.MalformedURLException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws MalformedURLException {
            Snap snap = new Snap();
            snap.clickOnChat();
            snap.clickOnNewChatButton();
            snap.sendMessageToEveryOne();
    }


}