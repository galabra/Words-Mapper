import org.openqa.selenium.WebElement;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowserWindow_UrlsMap {

    private final int APPEARANCES_TO_BECOME_URL = 2;

    private Map<String, String> urlsMap = new HashMap<>();
    private Map<String, Integer> wordsByFrequency;

    public BrowserWindow_UrlsMap(List<WebElement> urls) {
        for(WebElement we : urls) {
            urlsMap.put(we.getAttribute("textContent"), we.getAttribute("href"));
        }
    }

    public void addFrequentUrlsToUrlsFile(Map<String, Integer> wordsByFrequencyInput) {
        wordsByFrequency = wordsByFrequencyInput;

        Collection<String> urls = findFrequentUrls();
        addToUrlsTextFile(urls);
    }

    private Collection<String> findFrequentUrls() {
        HashMap<String, String> ansMap = new HashMap<>();

        // The equivalent HTML structure is: <a href="urlAddress"> urlLinkName </a>
        for (String urlLinkName : urlsMap.keySet()) {
            if (isValidUrl(urlLinkName)) {
                String urlAddress = urlsMap.get(urlLinkName);
                ansMap.put(urlLinkName, urlAddress);
            }
        }

        return ansMap.values();
    }

    private void addToUrlsTextFile(Collection<String> urls) {
        Main.urlsTextFile.appendLines(urls);
        Main.urls.addList(urls);
    }

    private boolean isValidUrl(String urlName) {
        return wordsByFrequency.containsKey(urlName) &&
               wordsByFrequency.get(urlName) > APPEARANCES_TO_BECOME_URL;
    }
	
}