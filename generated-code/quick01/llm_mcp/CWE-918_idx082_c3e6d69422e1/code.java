import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

private static List<Region> internalParse(final InputStream input,
                                          final boolean endpointVerification) throws IOException {
    try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        // Keep ownership of the input so a parser-initiated close cannot mask a parse failure.
        InputStream parserInput = new FilterInputStream(input) {
            @Override
            public void close() {
                // The original stream is closed in finally.
            }
        };
        Document document = factory.newDocumentBuilder().parse(parserInput);
        NodeList elements = document.getElementsByTagName(REGION_TAG);
        List<Region> regions = new ArrayList<>(elements.getLength());
        for (int i = 0; i < elements.getLength(); i++) {
            regions.add(parseRegionElement((Element) elements.item(i), endpointVerification));
        }
        return regions;
    } catch (IOException e) {
        throw e;
    } catch (Exception e) {
        throw new IOException("Failed to parse region metadata", e);
    } finally {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
                // Preserve the parse result or the original failure.
            }
        }
    }
}
