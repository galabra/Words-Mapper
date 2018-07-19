import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;

public class BrowserWindow {

	private FirefoxDriver driver = new FirefoxDriver();
    private Map<String, Integer> wordsByFrequency = new HashMap<>();
    private BrowserWindow_UrlsMap urlsMap;
    private String currentUrl;
    private boolean isHebrew;
    private final By bodyElement = By.tagName("body");

    BrowserWindow(String url) {
        isHebrew = url.matches("https?://he\\.wikipedia.+");
        this.currentUrl = url;
        browseTo(url);
    }

	public void browseTo(String url) {
        driver.navigate().to(url);
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(bodyElement));
    }

    public void updateWordsInDatabase() {
        Map<String, Word> wordsData = new HashMap();

        for (Map.Entry<String, Integer> entry : wordsByFrequency.entrySet()) {
            int numOfAppearances = entry.getValue();
            double ratio = numOfAppearances / (double) wordsByFrequency.size();
            Word currentWord = new Word(entry.getKey(), numOfAppearances, ratio);
            wordsData.put(entry.getKey(), currentWord);
        }

        Main.databaseFile.updateWordsInDatabase(wordsData);
    }

    public void close() {
        driver.close();
    }

    public void scrapeIntoWordsMap() {
        String bodyText = scrapeTextAndUrlsFromBody();
        buildWordsFrequencyMap(bodyText);
    }

    public void addUrlsToFile() {
        urlsMap.addFrequentUrlsToUrlsFile(wordsByFrequency);
    }

    private void buildWordsFrequencyMap(String text) {
        text = filterInvalidChars(text);
        String[] wordsList = text.split("\\s");

        for (String word : wordsList) {
            if (word.length() < 2) {
                continue;
            }
            if (isHebrew) {
                word = Hebrew.removeSpecialPrefixes(word);
            }

            if (wordsByFrequency.containsKey(word)) {
                incrementWordFrequency(word);
            }
            else {
                addNewWordIfValid(word);
            }
        }
    }

    private String filterInvalidChars(String text) {
        if (isHebrew) {
            text = text.replaceAll(getInvalidHebWikiExpressions(), "");
        }
        text = formatMultiwordExpressions(text);
        text = text.replaceAll("[^-\"'A-Za-z0-9\\s\\p{InHebrew}]", " ");
        return text;
    }

    private String getInvalidHebWikiExpressions() {
        StringBuilder ans = new StringBuilder();
        for(String exp : Hebrew.invalidWikiExpressions) {
            ans.append("(" + exp + ")|");
        }
        ans.deleteCharAt(ans.length() - 1);
        return ans.toString();
    }

    private String formatMultiwordExpressions(String text) {
        for(Map.Entry<String, String> curr : Hebrew.multiwordRegExpressions.entrySet()) {
            text = text.replaceAll(curr.getKey(), curr.getValue());
        }
        return text;
    }

    private String scrapeTextAndUrlsFromBody() {
        WebElement body;
        List<WebElement> urls;

        if(currentUrl.contains("wikipedia.org")) {
            String xpath = "//div[@class='mw-parser-output']";
            if(driver.findElements(By.xpath(xpath)).size() > 1) {
                xpath = "(" + xpath + ")[2]";
            }
            body = driver.findElement(By.xpath(xpath));
            urls = driver.findElements(By.xpath(xpath + "/*/a"));
        }
        else {
            body = driver.findElement(bodyElement);
            urls = driver.findElements(By.xpath("//a"));
        }
        urlsMap = new BrowserWindow_UrlsMap(urls);

        return body.getAttribute("textContent");
    }

    private void incrementWordFrequency(String word) {
	    int oldFrequency = wordsByFrequency.get(word);
        wordsByFrequency.put(word, oldFrequency + 1);
    }

    private void addNewWordIfValid(String word) {
        if (isValidWord(word)) {
            wordsByFrequency.put(word, 1);
        }
    }

    private boolean isValidWord(String word) {
        String formattedWord = word.toLowerCase().replaceAll("[^-\"'\\p{InHebrew}a-z0-9]", "");
        return formattedWord.matches("[-\"'a-z\\d\\p{InHebrew}]{2,20}") &&
                !formattedWord.matches("\\d+");
    }
	
}