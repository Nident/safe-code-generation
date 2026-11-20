public class ResponseParser {

	private static final String FAULT_CODE = "faultCode";
	private static final String FAULT_STRING = "faultString";

	
	public Object parse(SerializerHandler serializerHandler, InputStream response, boolean debugMode) throws XMLRPCException {

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

			
			
			factory.setFeature("http:
			factory.setExpandEntityReferences(false);
			factory.setNamespaceAware(true);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			

			DocumentBuilder builder = factory.newDocumentBuilder();
			Document dom = builder.parse(response);
			if (debugMode ){
				printDocument(dom, System.out);
			}
			Element e = dom.getDocumentElement();


			
			if(!e.getNodeName().equals(XMLRPCClient.METHOD_RESPONSE)) {
				throw new XMLRPCException("MethodResponse root tag is missing.");
			}

			e = XMLUtil.getOnlyChildElement(e.getChildNodes());

			if(e.getNodeName().equals(XMLRPCClient.PARAMS)) {

				e = XMLUtil.getOnlyChildElement(e.getChildNodes());

				if(!e.getNodeName().equals(XMLRPCClient.PARAM)) {
					throw new XMLRPCException("The params tag must contain a param tag.");
				}

				return getReturnValueFromElement(serializerHandler, e);

			} else if(e.getNodeName().equals(XMLRPCClient.FAULT)) {

				@SuppressWarnings("unchecked")
				Map<String,Object> o = (Map<String,Object>)getReturnValueFromElement(serializerHandler, e);

				throw new XMLRPCServerException((String)o.get(FAULT_STRING), (Integer)o.get(FAULT_CODE));

			}

			throw new XMLRPCException("The methodResponse tag must contain a fault or params tag.");

		} catch(XMLRPCServerException e) {
			throw e;
		} catch (Exception ex) {
			throw new XMLRPCException("Error getting result from server.", ex);
		}

	}

	public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http:

		transformer.transform(new DOMSource(doc),
				new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}

	
	private Object getReturnValueFromElement(SerializerHandler serializerHandler, Element element) throws XMLRPCException {

		Element childElement = XMLUtil.getOnlyChildElement(element.getChildNodes());

		return serializerHandler.deserialize(childElement);
	}

}