import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Hebrew extends Language {

    private static String languageName = "hebrew";
    public static ArrayList<String> grammarWords = new ArrayList<>(Arrays.asList("של", "על", "את", "הוא", "גם", "או", "הם", "בין", "יש", "עם", "זה", "היא", "כמו", "עד", "אך", "מי", "אל", "אם", "לא", "ניתן", "אשר", "יותר", "היו", "הן", "ביותר", "רק", "זו", "כאשר", "כך", "בהם", "כל", "באמצעות", "לפני", "כדי", "רוב", "כגון", "לאחר", "הרי", "אין", "היה", "לפי", "אינו", "אלו", "בתחום", "מן", "בעת", "בו", "ולכן", "כי", "הייתה", "בעיקר", "באופן", "שבו", "שבהם", "כיום", "בכל", "ולא", "אותם", "לדוגמה", "רבות", "למשל", "וכך", "על-ידי"));
    public static final Map<String, ArrayList<String>> wordsThatStartWithSpecialPrefixes = generateSpecialPrefixesMap();
    public static String[] invalidWikiExpressions = {
            "עריכת קוד מקור \\| עריכה",
            "תוכן עניינים \\[הסתרה]",
            "לקריאה נוספת",
            "קישורים חיצוניים",
            "ראו גם",
            " - מונחים",
            "ערך זה עוסק במונח ב",
            "אם התכוונתם למשמעות אחרת ? ראו",
            "פירושונים",
            "תמונות ומדיה בוויקישיתוף",
            "מיזמי קרן ויקימדיה",
            "ו?ויקי.+",
            "באתר של מכון דוידסון לחינוך מדעי",
            "למושגי יסוד נוספים בנושא",
            "ראו פורטל ה",
            "לקפוץ מעלה",
            "באתר",
            //"",
            "ערך מורחב"
    };

    public static final Map<String, String> multiwordRegExpressions = createMap();
    private static Map<String, String> createMap() {
        Map<String, String> map = new HashMap<>();
        map.put("על ידי", "על-ידי");
        map.put("ו?[לבמ]?בעלי ה?חיים", "בעלי-חיים");
        map.put(" את ה(.+) ([^ה])", " את $1 $2");
        return map;
    }

    public void uniteHebrewExpressions() {
        ArrayList<String> alreadyDealtWithWords = new ArrayList<>();
        boolean arrivedLastWord = false;

        while(! arrivedLastWord) {
            ArrayList<String> rawWordsList = Main.databaseFile.getColumnFromTable("Word", "Keywords", "");

            for (int i=0; i<rawWordsList.size(); i++) {
                if (i == rawWordsList.size() - 1) {
                    arrivedLastWord = true;
                }

                String currWord = rawWordsList.get(i);
                if (alreadyDealtWithWords.contains(currWord)) {
                    continue;
                }
                currWord = unwrap(currWord);

                ArrayList<String> wordsToHandle = new ArrayList<>();
                ArrayList<String> containsRawList = Main.databaseFile.getColumnFromTable("Word", "Keywords", "Word like '%" + currWord + "_'");

                String regex = "\\b" + "ו?(?:(?:כ?ש(ב|ה|מה)?)|(?:ל|ב|ה|מה?))(" + currWord + ")";
                for (String containsRaw : containsRawList) {
                    if (Utils.regexMatchesString(regex, containsRaw)) {
                        wordsToHandle.add(containsRaw);
                    }
                }

                if (Math.min(containsRawList.size(), wordsToHandle.size()) < 3) {
                    continue;
                }

                handleSimilarHebrewExpressions(currWord, wordsToHandle);
                alreadyDealtWithWords.add( wrap(currWord) );
                break;
            }
        }
    }

    private void handleSimilarHebrewExpressions(String currWord, ArrayList<String> wordsToHandle) {
        for(String similarWord : wordsToHandle) {
            System.out.println(currWord + "\t\t" + similarWord);
        }
    }

    public static void addNewWordsBySpecialPrefixes() {
        Logger.printWithTimeStamp("Started to add new words by their special prefixes");
        ArrayList<String> wordsList = Main.databaseFile.getColumnFromTable("Word", "Keywords", "");

        for (String prefix : wordsThatStartWithSpecialPrefixes.keySet()) {
            ArrayList<String> alreadyExist = wordsThatStartWithSpecialPrefixes.get(prefix);
            ArrayList<String> wordsToAdd = new ArrayList<>();
            String regex = "N'" + "ו?(?:כ?ש(?:מ?ׁׂה)|(?:מ?ה))(" + unwrap(prefix) + ".+?)'";

            for (int i = 0; i < wordsList.size(); i++) {
                String curr = wordsList.get(i);
                Matcher m = Pattern.compile(regex).matcher(curr);
                m.find();

                //if(m.group(1) != null) System.out.println(m.group(1));
                if (m.matches() && !alreadyExist.contains( wrap(m.group(1)) )) {
                    wordsToAdd.add( wrap(m.group(1)) );
                }
            }

            addWordsByPrefix(wordsToAdd, prefix);
            Logger.printWithTimeStamp(wordsToAdd.size() + " new words that start with '" + unwrap(prefix) + "' were added to the DB.");
        }
    }

    private static String unwrap(String rawWord) {
        if (rawWord.contains("N'")) {
            rawWord = rawWord.substring(2, rawWord.length() - 1); // N'xxx' --> xxx
        }
        return rawWord;
    }

    private static String wrap(String rawWord) {
        if (!rawWord.contains("N'")) {
            rawWord = "N'" + rawWord + "'";
        }
        return rawWord;
    }

    private static ArrayList<String> loadWordsByPrefix(String prefix) {
        return getColumnFromTable("Word", "WordsByPrefix", "Prefix='" + prefix.replaceAll("'", "''") + "'");
    }

    public static void addWordsByPrefix(ArrayList<String> values, String prefix) {
        String SEARCH_QUERY = "INSERT INTO WordsByPrefix (Word, Prefix) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
             PreparedStatement insertStmt = conn.prepareStatement(SEARCH_QUERY)) {
            for (String val : values) {
                insertStmt.setString(1, val);
                insertStmt.setString(2, prefix);
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void printWordsThatStartWithPrefix(String prefix) {
        ArrayList<String> wordsList = Main.databaseFile.getColumnFromTable("Word", "Keywords", "");
        String regex = "N'" + "ו?(?:כ?ש(?:מ?ׁׂה)|(?:מ?ה))(" + prefix + ".+?)'";

        for (String word : wordsList) {
            if (word.matches(regex)) {
                Matcher m = Pattern.compile(regex).matcher(word);
                if(m.find()) {
                    System.out.println(m.group(1));
                }
            }
        }
    }

    public static String removeSpecialPrefixes(String wordAsString) {
        for (String currentPrefix : wordsThatStartWithSpecialPrefixes.keySet()) {
            ArrayList<String> wordsThatStartWithCurrentPrefix = wordsThatStartWithSpecialPrefixes.get(currentPrefix);
            int length = unwrap(currentPrefix).length();

            String pre = wrap( wordAsString.substring(0, length) );
            String suf = wrap( wordAsString.substring(length) );

            if (pre.matches(currentPrefix) && wordsThatStartWithCurrentPrefix.contains(suf)) {
                return unwrap(suf);
            }
        }
        return wordAsString;
    }

    private static Map<String, ArrayList<String>> generateSpecialPrefixesMap() {
        makeSureLanguageExists(languageName);
        makeSurePrefixesTableIsNotEmpty();

        Map<String, ArrayList<String>> ans = new HashMap<>();

        List<String> prefixes = loadDifferentPrefixes();
        for(String prefix : prefixes) {
            ArrayList<String> wordsThatStartWithPrefix = loadWordsByPrefix(prefix);
            ans.put(prefix, wordsThatStartWithPrefix);
        }

        return ans;
    }

    private static void makeSurePrefixesTableIsNotEmpty() {
        ArrayList<String> arrH = new ArrayList<>();
        ArrayList<String> arrMH = new ArrayList<>();
        arrH.add(wrap("הסכם"));
        arrMH.add(wrap("מהומה"));
        addWordsByPrefix(arrH, wrap("ה"));
        addWordsByPrefix(arrMH, wrap("מה"));
    }

    private static List<String> loadDifferentPrefixes() {
        ArrayList<String> ans = new ArrayList<>();
        final String SEARCH_QUERY = "SELECT DISTINCT Prefix FROM WordsByPrefix";

        try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
             PreparedStatement searchStmt = conn.prepareStatement(SEARCH_QUERY)) {
            ResultSet result = searchStmt.executeQuery();
            while (result.next()) {
                String str = result.getString("Prefix");
                ans.add(str);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ans;
    }

}
