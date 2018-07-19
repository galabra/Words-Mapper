import java.io.UnsupportedEncodingException;

public class UrlWiki {

    private static String wikiIdentifier = "wikipedia.org/wiki/";
    private static String hebIdentifier = "he.wikipedia.org/wiki/";

    public static boolean isWikiPage(String url) {
        return url.contains(wikiIdentifier);
    }

    public static String extractPageName(String url) {
        if(url.contains(hebIdentifier)) {
            return extractHebrewPageName(url);
        }
        else {
            int nameBeginsAt = url.indexOf(wikiIdentifier) + wikiIdentifier.length();
            return url.substring(nameBeginsAt);
        }
    }

    private static String extractHebrewPageName(String url) {
        int nameBeginsAt = url.indexOf(hebIdentifier) + hebIdentifier.length();
        String encodedName = url.substring(nameBeginsAt);
        String decodedName = "";
        try {
            decodedName = java.net.URLDecoder.decode(encodedName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "N'" + decodedName + "'";
    }

}
