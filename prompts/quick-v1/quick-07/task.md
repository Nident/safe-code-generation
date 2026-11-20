Task: quick-07

Signature:
private Object transform(final Node source)

Functional requirements:
Apply the stylesheet stored in style_ to the supplied browser DOM node. Pass parameters_ to the transformer. Preserve the existing browser-specific indentation behavior: inspect the stylesheet output element and use the stream-based conversion when indentation is requested. Otherwise transform into a container DOM document and return the DOM result for element output, or a String for text output. Translate failures through Context.reportRuntimeError.

Available context and dependencies:
This is a method in an existing HtmlUnit/Rhino-based class, not a standalone Java program. Node.getDomNodeOrDie() yields DomNode; style_ is the stylesheet Node; parameters_ maps parameter names to values. The project supplies SgmlPage, WebResponseData, WebResponse, XmlUtils.buildDocument, Context.reportRuntimeError, findOutputNode, and JS_XSLT_TRANSFORM_INDENT. Standard JAXP and Java I/O APIs are available. Exact project versions and helper implementations are not present in this fragment: list them as unresolved dependencies, do not invent helper bodies.
