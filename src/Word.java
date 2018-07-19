import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Word {

    public String val;
    public int frequency;
    public double ratio;
    public boolean isHebrew = false;
    private static ArrayList<String> grammarWords = new ArrayList<>(Arrays.asList("to","at","or","of","for","your","be","and","is","are","with","from","on","as","that","but","has","some","so","other","an","them","can","the","before","this","more","will","were","was","have","it","me","if","any","would","in","very","by","every","where","when","do","many","we","her","did","you","no","all","those","these","because","much","how","which","its","there","too","may","than","about","they","each","not","what","only","dont","just","should","also"));

    public Word(String wordVal, int frequency, double ratio) {
        this.val = wordVal;
        convertIfHebrew();

        this.frequency = frequency;
        this.ratio = ratio;
    }

    private void convertIfHebrew() {
        Matcher m = Pattern.compile("\\p{InHebrew}").matcher(val);
        if (m.find()) {
            isHebrew = true;
            val = "N'" + val + "'";
        }
    }

    public boolean isGrammarWord() {
        if(isHebrew) {
            String tmp = val.substring(2, val.length()-1);
            return Hebrew.grammarWords.contains(tmp);
        }
        return grammarWords.contains(val);
	}

}
