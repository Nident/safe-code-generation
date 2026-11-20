public class XfaFile implements OutputStreamResource {

	
	protected Document xfaDocument;
	
	
	public XfaFile(OutputStreamResource resource) throws IOException, DocumentException {
		
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		resource.writeTo(baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		SAXReader reader = new SAXReader();
		xfaDocument = reader.read(bais);
	}

	
	public Document getXfaDocument() {
		return xfaDocument;
	}

	
	public void writeTo(OutputStream os) throws IOException {
		if (xfaDocument == null)
			return;
		OutputFormat format = new OutputFormat("   ", true);
        XMLWriter writer = new XMLWriter(os, format);
        writer.write(xfaDocument);
	}
}