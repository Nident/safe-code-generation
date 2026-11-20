import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

public Object getResourceDataForKey(String key) {
    Matcher separator = DATA_SEPARATOR_PATTERN.matcher(key);
    if (!separator.find()) {
        return null;
    }

    byte[] decoded;
    try {
        byte[] encoded = key.substring(separator.end()).getBytes(StandardCharsets.ISO_8859_1);
        decoded = decrypt(encoded);
    } catch (Exception e) {
        log.error(Messages.getMessage(Messages.RESOURCE_DATA_NOT_DECODED, key), e);
        return null;
    }

    if ("B".equals(separator.group(1)) || decoded == null) {
        return decoded;
    }

    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(decoded))) {
        return input.readObject();
    } catch (IOException | ClassNotFoundException e) {
        log.error(Messages.getMessage(Messages.RESOURCE_DATA_NOT_DECODED, key), e);
        return null;
    }
}
