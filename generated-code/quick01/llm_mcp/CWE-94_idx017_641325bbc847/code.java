import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

private Object transform(final Node source) {
    try {
        final DomNode sourceNode = source.getDomNodeOrDie();
        final DomNode stylesheet = style_.getDomNodeOrDie();

        final TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        final Transformer transformer = factory.newTransformer(new DOMSource(stylesheet));
        parameters_.forEach(transformer::setParameter);

        final DOMSource input = new DOMSource(sourceNode);
        final boolean textOutput = "text".equals(transformer.getOutputProperty(OutputKeys.METHOD));
        if (getBrowserVersion().hasFeature(JS_XSLT_TRANSFORM_INDENT)) {
            final org.w3c.dom.Node output = findOutputNode(stylesheet);
            if (output instanceof Element && "yes".equals(((Element) output).getAttribute("indent"))) {
                final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                transformer.transform(input, new StreamResult(stream));
                final byte[] bytes = stream.toByteArray();
                if (textOutput) {
                    final String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);
                    final Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
                    return new String(bytes, charset);
                }

                final SgmlPage page = sourceNode.getPage();
                final WebResponseData data = new WebResponseData(bytes, 200, "OK", Collections.emptyList());
                final WebResponse response = new WebResponse(data, page.getWebResponse().getWebRequest(), 0L);
                return XmlUtils.buildDocument(response);
            }
        }

        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Element container = document.createElement("container");
        document.appendChild(container);
        transformer.transform(input, new DOMResult(container));
        if (textOutput || (container.hasChildNodes() && container.getElementsByTagName("*").getLength() == 0)) {
            return container.getTextContent();
        }
        return container;
    }
    catch (final Exception | TransformerFactoryConfigurationError | FactoryConfigurationError e) {
        throw Context.reportRuntimeError(e.toString());
    }
}
