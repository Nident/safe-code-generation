import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

public static String sanitizeInput(String string) {
    if (string == null) {
        return null;
    }

    PolicyFactory policy = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.IMAGES);
    return policy.sanitize(string);
}
