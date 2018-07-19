import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Language {

    protected static String CONN_ADDRESS;

    protected static void makeSureLanguageExists(String languageName) {
        languageName = languageName.toLowerCase();
        File directory = new File("languages");
        if(! directory.exists()) directory.mkdir();

        File dataBaseFile = new File(directory.getAbsolutePath() + "/" + languageName + ".db");

        CONN_ADDRESS = "jdbc:sqlite:" + dataBaseFile.getAbsolutePath();

        if (! dataBaseFile.exists()) {
            try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
                 Statement statement = conn.createStatement()) {
                if (languageName.equals("hebrew")) {
                    String createTableQuery = "CREATE TABLE WordsByPrefix (Word nvarchar(255) NOT NULL," +
                                                                        "Prefix nvarchar(255) NOT NULL)";
                    statement.executeUpdate(createTableQuery);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    protected static ArrayList<String> getColumnFromTable(String column, String table, String wherePart) {
        final String where = (wherePart != null && wherePart.length() > 0) ? " WHERE " + wherePart : "";

        ArrayList<String> ans = new ArrayList<>();
        String SEARCH_QUERY = "SELECT " + column + " FROM " + table + where;

        try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
             PreparedStatement searchStmt = conn.prepareStatement(SEARCH_QUERY)) {
            ResultSet result = searchStmt.executeQuery();
            while (result.next()) {
                String str = result.getString(column);
                ans.add(str);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ans;
    }

    protected static void writeToColumnFromTable(String table, String column, ArrayList<String> values) {
        String SEARCH_QUERY = "INSERT INTO " + table + " (" + column + ") VALUES (?)";;

        try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
             PreparedStatement insertStmt = conn.prepareStatement(SEARCH_QUERY)) {
            for (String val : values) {
                insertStmt.setString(1, val);
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
