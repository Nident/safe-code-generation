@Override
    public void write(Document doc, OutputStream outputStream) throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try {
                factory.setAttribute("indent-number", 4);
            } catch (Exception e) {
                ; //guess we can't set it, that's ok
            }

            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, GlobalConfiguration.OUTPUT_FILE_ENCODING.getCurrentValue());

            //need to nest outputStreamWriter to get around JDK 5 bug.  See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, GlobalConfiguration.OUTPUT_FILE_ENCODING.getCurrentValue());
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            writer.flush();
            writer.close();
        } catch (TransformerException e) {
            throw new IOException(e.getMessage());
        }
    }