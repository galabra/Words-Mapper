import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class IOTextFile extends IOFile {

    IOTextFile(String filePath) {
        super(filePath);
    }

    public List<String> getLines() {
        Callable<List<String>> task = () -> {
            ArrayList<String> ans = new ArrayList<>();
            File inputFile = new File(name);
            try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if(!line.matches("\\s+"))
                        ans.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return ans;
        };

        Future<List<String>> future = tasksQueue.submit(task);
        return Utils.getFutureContent(future);
    }

    public int getLengthInLines() {
        Callable<Integer> task = () -> {
            int ans = 0;
            File inputFile = new File(name);
            try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if(!line.matches("\\s+"))
                        ans ++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return ans;
        };

        Future<Integer> future = tasksQueue.submit(task);
        return Utils.getFutureContent(future);
    }

    public void appendLines(Collection<String> urls2add) {
        Runnable task = () -> {
            if (urls2add.size() > 0) {
                tasksQueue.submit(() -> {
                    try (FileWriter fw = new FileWriter(name, true);
                         BufferedWriter bw = new BufferedWriter(fw);
                         PrintWriter out = new PrintWriter(bw)) {
                        out.print("\n");
                        for (String url : urls2add) {
                            String toPrint = UrlWiki.isWikiPage(url) ? UrlWiki.extractPageName(url) : url;
                            Logger.printNewUrl(toPrint);
                            out.println(url);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        };

        tasksQueue.submit(task);
    }

}