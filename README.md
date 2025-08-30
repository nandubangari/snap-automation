
# Snap Mobile(Android) Automation Bot - Appium

> Automated Snapchat chat operations (open chat, new chat, send messages, optional unfriend flows) built with **Java + Appium**.  
> Config-driven, resilient to transient Appium/driver failures, and designed to minimize flaky failures via re-finds, retries, and driver-repair logic.

---

## Table of Contents
1. [Overview](#overview)
2. [Features](#features)
3. [Prerequisites](#prerequisites)
4. [Project structure](#project-structure)
5. [Configuration](#configuration)
6. [Logging](#logging)
7. [Build & Run](#build--run)
8. [Main components (files explained)](#main-components-files-explained)
9. [How the flow works](#how-the-flow-works)
10. [Business rules & customizations](#business-rules--customizations)
11. [Troubleshooting & common fixes](#troubleshooting--common-fixes)
12. [Best practices & recommendations](#best-practices--recommendations)
13. [Contributing](#contributing)
14. [License & Disclaimer](#license--disclaimer)

---

## Overview
This project automates messaging flows on Snapchat for testing/demo purposes. It locates sections (A–Z and `#`) and iterates through friends in each section, either sending a message or performing a configurable "unfriend" action depending on rules specified in `config.properties`.

Key goals:
- Keep code config-driven.
- Minimize flakiness: re-find elements before using, robust waits, safe swipe/scroll with retries, driver repair.
- Centralize rules (skip/unfriend/send decisions) in `config.properties`.

---

## Features
- Discover **sections A–Z & `#`** (XPath `matches()` fast path with a Java fallback).
- Iterate friends per section with re-finds to avoid `StaleElementReferenceException`.
- Scroll elements safely using bounds when available.
- Resilient driver repair (`repairDriverSafely`) and short retries for transient failures.
- Central configuration (`config.properties`) for device, Appium, message text, and business rules.
- Logging via **SLF4J + Logback** with file + console outputs.
- `close()` method in `Snap` for clean shutdown.

---

## Prerequisites
- Java 11+ (or as compatible with your chosen `java-client`)
- Maven (or Gradle)
- Appium server installed and running
- Android device/emulator connected and available via `adb devices`
- Proper SDK/platform tools for the device/emulator

> **Note:** Appium, Node, and java-client compatibility matters. If you upgrade any of these, verify compatibility between Appium server and `io.appium:java-client` you're using.

---

## Project structure
```
project-root/
├─ pom.xml
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  ├─ com/app/Main.java
│  │  │  ├─ com/snap/Snap.java
│  │  │  ├─ com/utils/ConfigManager.java
│  │  │  └─ com/utils/Utils.java
│  │  └─ resources/
│  │     ├─ config.properties
│  │     └─ logback.xml
└─ README.md
```

---

## Configuration

Create `src/main/resources/config.properties` and change values to match your environment. Example:

```properties
# Messaging rules
message.to.send=Hey! This is an automated test message 🚀
skip.if.contains=Do not disturb,Busy,Already Sent
# optional: restrict to certain friends, name contains
send.only.to=

# ---------- Unfriend Rules ----------
unfriend.enable=true
unfriend.skip.if.contains=Best Friend,Pinned,Family
```

**Notes**
- `skip.if.contains` and `unfriend.skip.if.contains` accept comma-separated words/phrases. Comparisons are case-insensitive and treat any match in the last message as a "skip".
- `send.only.to` if non-empty restricts sending to only the listed names.

---

## Logging

Place `logback.xml` in `src/main/resources`. Example configuration (console + rolling file):

```xml
<!-- src/main/resources/logback.xml (example) -->
<configuration>
  <property name="LOG_DIR" value="${LOG_DIR:-logs}" />
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder><pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern></encoder>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_DIR}/snap.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>${LOG_DIR}/snap.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
      <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
        <maxFileSize>50MB</maxFileSize>
      </timeBasedFileNamingAndTriggeringPolicy>
      <maxHistory>14</maxHistory>
    </rollingPolicy>
    <encoder><pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern></encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
  </root>

  <logger name="com.snap" level="DEBUG"/>
</configuration>
```

**Important**: If logs don't appear:
- Ensure `logback-classic` is the only SLF4J provider on your classpath (see Troubleshooting).

---

## Build & Run

### Start Appium & device
- Start Appium server:
```bash
appium
```
- Make sure device is connected:
```bash
adb devices
```

### Maven (recommended)
1. Build:
```bash
mvn clean package
```

2. Run (example using exec plugin):
```bash
mvn exec:java -Dexec.mainClass="com.app.Main"
```

Alternatively run the JAR:
```bash
java -jar target/your-artifact.jar
```



---

## Main components (files explained)

### `Main.java`
- Entry point. Creates `Snap`, invokes flows (`clickOnChat()`, `clickOnNewChatButton()`, `sendMessageToEveryOne()`), and calls `snap.close()` in `finally` to quit driver cleanly.

### `ConfigManager.java`
- Loads `config.properties` and exposes `get`, `getInt`, `getBoolean` helpers.

### `Utils.java`
- Central utility wrapper around `AndroidDriver`.
- Robust wait methods, safe `perform()` wrapper for touch/swipe, `repairDriverSafely()` to recreate sessions on session drops.
- Methods: `clickElement`, `setText`, `hideKeyBoard`, `navigateBack`, `pressEnter`, `waitUntilElementDisappears`, `scrollElementIntoViewSafe`, etc.

### `Snap.java`
- High-level page flow: finds sections (A–Z/#), iterates friends, decides whether to send message or unfriend according to config rules, uses `Utils` for all interactions.
- Implements a `close()` for graceful shutdown.

---

## How the flow works
1. `Main` starts and creates `Snap`.
2. `Snap` initializes `Utils` (which creates the `AndroidDriver`).
3. `Snap.sendMessageToEveryOne()`:
    - Gets all sections using `sectionsBy` XPath (`matches(@resource-id, '^([A-Z]|#)$')`) — if XPath fails or returns empty, a Java fallback scans `android.view.View` elements and filters resource-id tokens to A–Z/#.
    - For each section:
        - Snapshot friends inside the section (re-fetched frequently to avoid stale references).
        - For each friend:
            - Get friend name safely.
            - Scroll into view (bounds-based), click friend name to open chat.
            - Use `config.properties` rules to decide:
                - Skip sending if last message contains any skip keywords.
                - If friend, send configured messages.
                - If not friend and `unfriend.enable=true` and not matching `unfriend.skip.if.contains`, perform unfriend flow.
    - All interactions use `Utils` which handles retries and driver repair.

---

## Business rules & customizations
You can tweak behavior without code changes via `config.properties`:
- `message.to.send` — change message content.
- `skip.if.contains` — keywords to avoid sending messages.
- `send.only.to` — restrict sending to specific names.
- `unfriend.enable` — turn on/off the unfriend action.
- `unfriend.skip.if.contains` / `dont.unfriend.if.lastMessageContains` — protect certain contacts.

If you need more advanced rules (regex, age-of-last-message checks), add fields to `config.properties` and implement parsing logic in `Snap.java`.

---

## Troubleshooting & common fixes

### 1. Elements not found / stale
- Re-run and check UI changes; sometimes Snapchat layout changes.
- Use `adb shell uiautomator dump` or Appium Desktop inspector to inspect current resource-ids.
- Increase `explicitWait` in config, or adjust `scrollElementIntoViewSafe` safe zones.

### 2. Appium session drops / driver failures
- `Utils` attempts `repairDriverSafely` and reinitializes `Utils`. If repair can't recreate the session, check server/device (Appium logs, `adb devices`).
- If repairing fails repeatedly, verify Appium server is running and the device is connected.

### 3. Logs not writing to file
- Ensure `logback.xml` is on classpath (`src/main/resources`).
- Ensure the process user has permission to create `logs` directory (or set an absolute `log.dir` in config).

---

## Best practices & recommendations
- Keep `config.properties` out of VCS for sensitive values (or use environment-specific files).
- Add a `sleep` between messages to avoid rate-limiting / suspicious behavior.
- Use a dedicated test account on Snapchat (do not automate your personal account).
- Add retry counters and fail-fast thresholds to avoid infinite loops.
- Consider storing processed friend list (checkpoint) to resume after crash.

---

## Contributing
1. Fork repo
2. Create feature branch
3. Run tests & verify against emulator/device
4. Open PR with explanation and relevant screenshots/logs

---

## License & Disclaimer
**License**: MIT (or choose your preferred license)

**Disclaimer**: This project is for **educational / testing** purposes only. Automating interactions with production services (including Snapchat) may violate terms of service. Use responsibly and at your own risk.

---
