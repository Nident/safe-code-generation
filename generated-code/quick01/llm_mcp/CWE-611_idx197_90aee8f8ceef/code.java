import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

private Document loadXmlFrom(InputStream is) throws SAXException, IOException {
    if (is == null) {
        throw new IllegalArgumentException("Input stream must not be null");
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException | IllegalArgumentException e) {
        throw new SAXException("Could not configure the XML parser", e);
    }

    return builder.parse(new FilterInputStream(is) {
        @Override
        public void close() {
            // The caller owns the underlying stream.
        }
    });
}
