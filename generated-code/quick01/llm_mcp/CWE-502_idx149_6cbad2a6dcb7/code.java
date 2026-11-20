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

    byte[] encoded = key.substring(separator.end()).getBytes(StandardCharsets.ISO_8859_1);
    byte[] decoded;
    try {
        decoded = decrypt(encoded);
    } catch (Exception failure) {
        log.error(Messages.getMessage(Messages.RESOURCE_DESERIALIZATION_ERROR), failure);
        return null;
    }

    if (decoded == null || "B".equals(separator.group(1))) {
        return decoded;
    }

    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(decoded))) {
        return input.readObject();
    } catch (IOException | ClassNotFoundException failure) {
        log.error(Messages.getMessage(Messages.RESOURCE_DESERIALIZATION_ERROR), failure);
        return null;
    }
}
