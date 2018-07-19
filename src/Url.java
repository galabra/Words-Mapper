import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Url {

    public String asString;
    private String domain;
    private String pageName;

    public Url(String urlAsString) {
        this.asString = urlAsString;
        this.domain = extractDomain();
        this.pageName = extractPageName();
    }

    public boolean isEmptyUrl() {
        return asString.length() < 3;
    }

    public Runnable scrapeWebpageIntoDatabase() {
        String decodedUrl = decode(this.asString);

        return () -> {
            Logger.printStartedUrl(decodedUrl);

            BrowserWindow window = new BrowserWindow(this.asString);
            window.scrapeIntoWordsMap();
            window.addUrlsToFile();
            window.updateWordsInDatabase();
            window.close();

            addToVisitedWebpages();
            Logger.printFinishedUrl(decodedUrl);
            Main.urls.countDown();
        };
    }

    private String decode(String encoded) {
        String ans = null;
        try {
            ans = java.net.URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ans;
    }

    private String extractDomain() {
        String domainRegex = "(?:https?:\\/{2})?(?:www\\.)?([\\w-]+\\.[A-Za-z.]+)(?:\\/)";
        return applyRegexOnInput(domainRegex, asString);
    }

    private String extractPageName() {
        if(UrlWiki.isWikiPage(asString)) {
            return UrlWiki.extractPageName(asString);
        }
        else {
            String pageNameRegex = "(?:.+\\/)([\\w-]+)(?:\\/|\\.\\w{3,})?$";
            return applyRegexOnInput(pageNameRegex, asString);
        }
    }

    private String applyRegexOnInput(String regex, String input) {
        String ans = input;
        Matcher m = Pattern.compile(regex).matcher(input);
        if (m.find())
            ans = m.group(1);
        return ans;
    }

    private void addToVisitedWebpages() {
        HashMap<String, String> values = new HashMap<>();
        values.put("Full_URL", asString);
        values.put("Domain",   domain);
        values.put("PageName", pageName);

        Main.urlsDatabaseFile.insertNewValuesToTable(values, "VisitedWebpages");
    }

}
