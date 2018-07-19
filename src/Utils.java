import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static boolean regexMatchesString(String regex, String input) {
        Matcher m = Pattern.compile(regex).matcher(input);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static <T> T getFutureContent(Future<T> future) {
        T ans = null;
        try {
            ans = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return ans;
    }

}
