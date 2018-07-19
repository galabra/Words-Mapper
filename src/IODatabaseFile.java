import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class IODatabaseFile extends IOFile {

    private String CONN_ADDRESS;

    public IODatabaseFile(String filePath) {
        super(filePath);
        CONN_ADDRESS = "jdbc:sqlite:" + name;

        File dataBaseFile = new File(name);
        if (!dataBaseFile.exists()) {
            createNewTables();
        }
    }

    private void createNewTables() {
        try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
             Statement statement = conn.createStatement()) {
            if (name.equals("visitedUrls.db")) {
                String createWebpagesQuery = "CREATE TABLE VisitedWebpages (Full_URL varchar(255) NOT NULL, " +
                                                                            "Domain varchar(255) NOT NULL, " +
                                                                            "PageName varchar(255) NOT NULL)";
                statement.executeUpdate(createWebpagesQuery);
            }
            else if (name.equals("database.db")) {
                String[] tablesNames = {"Words", "Keywords", "Grammar_Words"};
                for (String tableName : tablesNames) {
                    String createTableQuery = getNewWordsTableQuery(tableName);
                    statement.executeUpdate(createTableQuery);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String getNewWordsTableQuery(String tableName) {
        return "CREATE TABLE " + tableName + " (Word varchar(255) NOT NULL, " +
                "TotalAppearances int, " +
                "MinRatioPerPage int, " +
                "AvgRatioPerPage int, " +
                "URLsAmount int)";
    }

    public <S> ArrayList<S> getColumnFromTable(String column, String table, String wherePart) {
        Future<ArrayList<S>> future = tasksQueue.submit(selectQueryCallable(column, table, wherePart));
        ArrayList<S> ans = Utils.getFutureContent(future);
        return ans;
    }

    private <S> Callable<ArrayList<S>> selectQueryCallable(String column, String table, String wherePart) {
        final String where = (wherePart != null && wherePart.length() > 0) ? " WHERE " + wherePart : "";

        return () -> {
            ArrayList<S> futureAns = new ArrayList<>();
            String SEARCH_QUERY = "SELECT " + column + " FROM " + table + where;

            try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
                 PreparedStatement searchStmt = conn.prepareStatement(SEARCH_QUERY)) {
                ResultSet result = searchStmt.executeQuery();
                while (result.next()) {
                    S str = (S) result.getObject(column);
                    futureAns.add(str);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return futureAns;
        };
    }

    public void insertNewValuesToTable(Map<String, String> values, String tableName) {
        String SQL_INSERT = getNewInsertQuery(values, tableName);

        Runnable task = () -> {
            try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
                 PreparedStatement insertStmt = conn.prepareStatement(SQL_INSERT)) {
                int i = 1;
                for(Iterator<String> iterator = values.values().iterator(); iterator.hasNext();) {
                    String curr = iterator.next();
                    insertStmt.setString(i, curr);
                    i ++;
                }
                insertStmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        tasksQueue.submit(task);
    }

    private String getNewInsertQuery(Map<String, String> values, String tableName) {
        // For example: "INSERT INTO VisitedWebpages (Full_URL, Domain, PageName) VALUES (?, ?, ?)";

        StringBuilder queryBuilder = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder valuesBuilder = new StringBuilder();

        int size = values.size();
        int i = 0;
        for(Map.Entry<String, String> entry : values.entrySet()) {
            queryBuilder.append(entry.getKey());
            valuesBuilder.append("?");
            if (i < size - 1) {
                queryBuilder.append(", ");
                valuesBuilder.append(", ");
            }
            i ++;
        }
        return (queryBuilder + ") VALUES (" + valuesBuilder + ")");
    }

    public synchronized void updateWordsInDatabase(Map<String, Word> wordsByFrequency) {
        // Note this method is synchronized!

        Callable filterWordsToLists = filterWordsToLists(wordsByFrequency);
        Future<List<List<Word>>> future = tasksQueue.submit(filterWordsToLists);
        List<List<Word>> wordsLists = Utils.getFutureContent(future);

        List<Word> newWords = wordsLists.get(0);
        List<Word> existingWords = wordsLists.get(1);

        if (!newWords.isEmpty()) insertIntoDatabase(newWords);
        if (!existingWords.isEmpty()) updateInDatabase(existingWords);
    }

    private Callable<List<List<Word>>> filterWordsToLists(Map<String, Word> wordsByFrequency) {
        return () -> {
            ArrayList newWords2Insert = new ArrayList();
            ArrayList existingWords2Update = new ArrayList();

            String SEARCH_QUERY = "SELECT * from Words WHERE Word=?";
            try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
                 PreparedStatement searchStmt = conn.prepareStatement(SEARCH_QUERY)) {

                for (Map.Entry<String, Word> entry : wordsByFrequency.entrySet()) {
                    Word currentWord = entry.getValue();
                    searchStmt.setString(1, "N'" + entry.getKey() + "'");
                    ResultSet result = searchStmt.executeQuery();

                    if(result.next()) {
                        existingWords2Update.add(currentWord);
                    }
                    else {
                        newWords2Insert.add(currentWord);
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            ArrayList futureAns = new ArrayList();
            futureAns.add(newWords2Insert);
            futureAns.add(existingWords2Update);

            return futureAns;
        };
    }

    private void insertIntoDatabase(List<Word> words) {
        String SQL_INSERT_Words   = "INSERT INTO Words (URLsAmount, Word, TotalAppearances, MinRatioPerPage, AvgRatioPerPage) VALUES (1, ?, ?, ?, ?)";
        String SQL_INSERT_Grammar = "INSERT INTO Grammar_Words (URLsAmount, Word, TotalAppearances, MinRatioPerPage, AvgRatioPerPage) VALUES (1, ?, ?, ?, ?)";
        String SQL_INSERT_Keyword = "INSERT INTO Keywords (URLsAmount, Word, TotalAppearances, MinRatioPerPage, AvgRatioPerPage) VALUES (1, ?, ?, ?, ?)";

        Runnable task = () -> {
            try (Connection conn = DriverManager.getConnection(CONN_ADDRESS);
                 PreparedStatement insertStmt1 = conn.prepareStatement(SQL_INSERT_Words);
                 PreparedStatement insertStmt_grammar = conn.prepareStatement(SQL_INSERT_Grammar);
                 PreparedStatement insertStmt_keyword = conn.prepareStatement(SQL_INSERT_Keyword)) {

                for(Word word : words) {
                    insertStmt1.setString(1, word.val);
                    insertStmt1.setString(2, String.valueOf(word.frequency));
                    insertStmt1.setString(3, String.valueOf(word.ratio));
                    insertStmt1.setString(4, String.valueOf(word.ratio));
                    insertStmt1.executeUpdate();

                    PreparedStatement insertStmt2 = word.isGrammarWord() ? insertStmt_grammar : insertStmt_keyword;
                    insertStmt2.setString(1, word.val);
                    insertStmt2.setString(2, String.valueOf(word.frequency));
                    insertStmt2.setString(3, String.valueOf(word.ratio));
                    insertStmt2.setString(4, String.valueOf(word.ratio));
                    insertStmt2.executeUpdate();
                }

                Logger.printInsertedNewWordsMsg();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        tasksQueue.submit(task);
    }

    private void updateInDatabase(List<Word> words) {
        String[] SQL_UPDATE = new String[9];
        String[] tableNames = {"Words", "Grammar_Words", "Keywords"};

        for (int i=0; i<7; i+=3) {
            SQL_UPDATE[i+0] = "UPDATE " + tableNames[i/3] + " SET " +
                    "TotalAppearances=TotalAppearances+?, URLsAmount=URLsAmount+1 WHERE Word=?";
            SQL_UPDATE[i+1] = "UPDATE " + tableNames[i/3] + " SET MinRatioPerPage=? " +
                    "WHERE Word=? AND MinRatioPerPage>?";
            SQL_UPDATE[i+2] = "UPDATE " + tableNames[i/3] + " SET AvgRatioPerPage=(? + (URLsAmount * AvgRatioPerPage))/URLsAmount " +
                    "WHERE Word=?";
        }

        Runnable task = () -> {
            try (Connection conn = DriverManager.getConnection(CONN_ADDRESS)) {
                PreparedStatement[] psArr = new PreparedStatement[9];
                for(int i=0; i<9; i++) {
                    psArr[i] = conn.prepareStatement(SQL_UPDATE[i]);
                }

                int[] tables = new int[2];
                tables[0] = 0;
                for(Word word : words) {
                    tables[1] = word.isGrammarWord() ? 1 : 2;

                    for (int tableIndex : tables) {
                        psArr[tableIndex*3 + 0].setString(1, String.valueOf(word.frequency));
                        psArr[tableIndex*3 + 0].setString(2, word.val);

                        psArr[tableIndex*3 + 1].setString(1, String.valueOf(word.ratio));
                        psArr[tableIndex*3 + 1].setString(2, word.val);
                        psArr[tableIndex*3 + 1].setString(3, String.valueOf(word.ratio));

                        psArr[tableIndex*3 + 2].setString(1, String.valueOf(word.ratio));
                        psArr[tableIndex*3 + 2].setString(2, word.val);

                        psArr[tableIndex*3 + 0].executeUpdate();
                        psArr[tableIndex*3 + 1].executeUpdate();
                        psArr[tableIndex*3 + 2].executeUpdate();
                    }
                }

                for(int i=0; i<9; i++) {
                    psArr[i].close();
                }

                Logger.printUpdatedExistingWordsMsg();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        tasksQueue.submit(task);
    }

}