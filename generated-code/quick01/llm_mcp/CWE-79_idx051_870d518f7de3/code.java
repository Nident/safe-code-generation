import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

public static String sanitizeInput(String string) {
    if (string == null) {
        return null;
    }

    Document input = Jsoup.parseBodyFragment(string);
    input.getElementsByTag("script").remove();

    Document sanitized = new Cleaner(Safelist.basic()).clean(input);
    sanitized.outputSettings().prettyPrint(false);
    return sanitized.body().html();
}
