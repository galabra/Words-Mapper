import java.io.File;
import java.util.concurrent.*;

public class IOFile {

    protected String name;
    ExecutorService tasksQueue = Executors.newSingleThreadExecutor();

    IOFile(String filePath) {
        File directory = new File("database_files");
        if(! directory.exists()) directory.mkdir();
        this.name = directory.getAbsolutePath() + "/" + filePath;
    }

	public void shutdown() {
        tasksQueue.shutdown();
        try {
            tasksQueue.awaitTermination(Main.AWAIT_UNTIL_TERMINATION_in_minutes, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}