import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class UrlsQueue {

    private BlockingQueue<String> urls = new LinkedBlockingQueue();
    private static int urlsUntilTermination = Main.URLS_THRESHOLD;
    private CountDownLatch counter_for_UrlsAwait;
    private ArrayList<String> existingUrls_asStrings = Main.urlsDatabaseFile.getColumnFromTable("Full_URL", "VisitedWebpages", "");

    public UrlsQueue(List<String> lst) {
        urls.addAll(lst);
    }

    public synchronized void addList(Collection<String> urlsToAdd) {
        urls.addAll(urlsToAdd);
    }

    public void countDown() {
        counter_for_UrlsAwait.countDown();
    }

    public void scanUrls() {
        counter_for_UrlsAwait = new CountDownLatch(Main.URLS_THRESHOLD);

        while (urlsUntilTermination > 0) {
            Url currentUrl = fetchNewUrlFromQueue();

            if (isValidUrl(currentUrl.asString)) {
                handleNewUrl(currentUrl);
                existingUrls_asStrings.add(currentUrl.asString);
                urlsUntilTermination --;
            }
        }
        awaitCurrentIterationUrls();
    }

    private boolean isValidUrl(String urlAsString) {
        boolean test1 = !existingUrls_asStrings.contains(urlAsString);
        boolean test2 = urlAsString.contains(".wikipedia.org/wiki/");
        return test1 && test2;
    }

    private Url fetchNewUrlFromQueue() {
        Url ans = null;
        try {
            ans = new Url(urls.take());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ans;
    }

    private void handleNewUrl(Url url) {
        if(url.isEmptyUrl()) {
            // don't count empty URLs
            urlsUntilTermination ++;
        }
        else {
            Runnable handleWebpage = url.scrapeWebpageIntoDatabase();
            Main.pool.submit(handleWebpage);
        }
    }

    private void awaitCurrentIterationUrls() {
        try {
            Logger.printWithTimeStamp("Awaiting a " + counter_for_UrlsAwait.getCount() + " URLs batch");
            counter_for_UrlsAwait.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
