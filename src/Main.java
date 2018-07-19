import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static int POOL_SIZE = 10;
    public static int URLS_THRESHOLD = 1;
    public static int AWAIT_UNTIL_TERMINATION_in_minutes = 1;

    public static final IODatabaseFile urlsDatabaseFile = new IODatabaseFile("visitedUrls.db");
    public static final IOTextFile urlsTextFile = new IOTextFile("input_urls.txt");
    public static final IODatabaseFile databaseFile = new IODatabaseFile("database.db");
    public static final ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
    public static UrlsQueue urls = new UrlsQueue(urlsTextFile.getLines());

	public static void main(String[] args) {
        Logger.initialize_TestingModeIs(false);

        urls.scanUrls();

        Hebrew.addNewWordsBySpecialPrefixes();
        shutdownPools();
        Logger.printWithTimeStamp("Finished running. Shutting down.");
    }

    private static void shutdownPools() {
        databaseFile.shutdown();
        urlsDatabaseFile.shutdown();
        urlsTextFile.shutdown();
        pool.shutdown();

        try {
            pool.awaitTermination(AWAIT_UNTIL_TERMINATION_in_minutes, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}