import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

private Document loadXmlFrom(InputStream is) throws SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);

    try {
        return factory.newDocumentBuilder().parse(new FilterInputStream(is) {
            @Override
            public void close() {
                // The caller owns the underlying stream.
            }
        });
    } catch (ParserConfigurationException e) {
        throw new SAXException("Could not configure a namespace-aware XML parser", e);
    }
}
