import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.javascript.host.dom.Node;
import com.gargoylesoftware.htmlunit.xml.XmlUtils;

import net.sourceforge.htmlunit.corejs.javascript.Context;

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_XSLT_TRANSFORM_INDENT;

private Object transform(final Node source) {
    try {
        final DomNode sourceDomNode = source.getDomNodeOrDie();
        final DomNode stylesheet = style_.getDomNodeOrDie();
        final Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(new DOMSource(stylesheet));
        parameters_.forEach(transformer::setParameter);

        final DOMSource input = new DOMSource(sourceDomNode);
        final SgmlPage page = sourceDomNode.getPage();
        final org.w3c.dom.Node output = findOutputNode(stylesheet);
        final org.w3c.dom.Node indent = output == null ? null : output.getAttributes().getNamedItem("indent");
        if (indent != null && "yes".equals(indent.getNodeValue())
                && page.getWebClient().getBrowserVersion().hasFeature(JS_XSLT_TRANSFORM_INDENT)) {
            if ("text".equals(transformer.getOutputProperty(OutputKeys.METHOD))) {
                final StringWriter text = new StringWriter();
                transformer.transform(input, new StreamResult(text));
                return text.toString();
            }

            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            transformer.transform(input, new StreamResult(bytes));
            final WebResponseData data = new WebResponseData(bytes.toByteArray(), 200, "OK",
                    Collections.emptyList());
            final WebResponse response = new WebResponse(data, page.getUrl(), HttpMethod.GET, 0);
            return XmlUtils.buildDocument(response);
        }

        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Element container = document.createElement("container");
        document.appendChild(container);
        final DOMResult result = new DOMResult(container);
        transformer.transform(input, result);

        final org.w3c.dom.Node resultNode = result.getNode();
        for (org.w3c.dom.Node child = resultNode.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                return resultNode;
            }
        }
        return resultNode.getTextContent();
    }
    catch (final Exception e) {
        throw Context.reportRuntimeError(e.getMessage() == null ? e.toString() : e.getMessage());
    }
}
