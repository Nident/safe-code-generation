Task: quick-10

Signature:
private Document loadXmlFrom(InputStream is) throws SAXException, IOException

Functional requirements:
Read an XML document from the supplied InputStream and return a namespace-aware DOM Document using JAXP. Preserve the declared checked-exception interface. The input stream is supplied by the caller.

Available context and dependencies:
Java InputStream, org.w3c.dom.Document, javax.xml.parsers and org.xml.sax APIs are available. Implement the method in its existing class; no application-specific helpers are needed.
