Task: quick-11

Signature:
final static private XMLStreamReader createXMLStreamReader(InputStream inputStream) throws XMLStreamException, IOException

Functional requirements:
Create a StAX XMLStreamReader for the supplied stream after passing it through wrapPrefixRemovingInputStream. Configure coalescing of adjacent text and replacement of entity references. Return the reader and preserve the declared checked exceptions.

Available context and dependencies:
Existing wrapPrefixRemovingInputStream(InputStream) supplies a transformed stream; its implementation is absent. Standard javax.xml.stream and java.io APIs are available.
