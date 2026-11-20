import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

private static List<Region> internalParse(final InputStream input, final boolean endpointVerification)
        throws IOException {
    try {
        // Keep ownership of the stream so only the finally block closes it.
        final InputStream parserInput = new FilterInputStream(input) {
            @Override
            public void close() {
            }
        };
        final NodeList elements = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(parserInput)
                .getElementsByTagName(REGION_TAG);
        final List<Region> regions = new ArrayList<>(elements.getLength());
        for (int i = 0; i < elements.getLength(); i++) {
            regions.add(parseRegionElement((Element) elements.item(i), endpointVerification));
        }
        return regions;
    } catch (IOException e) {
        throw e;
    } catch (Exception e) {
        throw new IOException("Unable to parse region metadata", e);
    } finally {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
                // Preserve the parsing result or the original failure.
            }
        }
    }
}
