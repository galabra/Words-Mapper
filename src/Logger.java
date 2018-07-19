import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Logger {

    private static long initialTime;
    private static boolean testingMode = false;
    private static AtomicInteger startedCounter = new AtomicInteger();
    private static AtomicInteger finishedCounter = new AtomicInteger();
    private static Map<String, Integer> currentlyActive = new ConcurrentHashMap<>();

    public static void initialize_TestingModeIs(boolean isTesting) {
        testingMode = isTesting;
        initialTime = System.currentTimeMillis()/1000;
        disableSeleniumLogs();
    }

    private static void disableSeleniumLogs() {
        System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE,"true");
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"/dev/null");
    }

    public static void printWithTimeStamp(String msg) {
        long timePassedInSeconds = System.currentTimeMillis()/1000 - initialTime;
        long timePassedInMinutes = timePassedInSeconds/60;
        long deltaSeconds = timePassedInSeconds - timePassedInMinutes*60;

        System.out.println(
                (timePassedInMinutes<10 ? "0"+timePassedInMinutes : timePassedInMinutes)
                        + ":" +
                        (deltaSeconds<10 ? "0"+deltaSeconds : deltaSeconds) + "  >>  " + msg);
    }

    public static void printStartedUrl(String url) {
        int currentId = startedCounter.incrementAndGet();
        String location = currentId + "/" + Main.URLS_THRESHOLD;
        currentlyActive.put(url, currentId);
        Logger.printWithTimeStamp(" Started scraping URL #" + currentId + " [" + location + "]:  " + url);
    }

    public static void printFinishedUrl(String url) {
        int currentId = currentlyActive.get(url);
        String location = finishedCounter.incrementAndGet() + "/" + Main.URLS_THRESHOLD;
        currentlyActive.remove(url);
        Logger.printWithTimeStamp("Finished scraping URL #" + currentId + " [" + location + "]:  " + url);
    }

    public static void printNewUrl(String url) {
        if (testingMode) {
            printWithTimeStamp("Added new URL: " + url);
        }
    }

    public static void printInsertedNewWordsMsg() {
        if (testingMode) {
            printWithTimeStamp("Inserted new words into DB");
        }
    }

    public static void printUpdatedExistingWordsMsg() {
        if (testingMode) {
            printWithTimeStamp("Updated words in DB");
        }
    }

}