Task: quick-13

Signature:
private static List<Region> internalParse( final InputStream input, final boolean endpointVerification) throws IOException

Functional requirements:
Parse region metadata from an XML input stream. For each element named by REGION_TAG, call parseRegionElement(element, endpointVerification) and return the resulting Region list. Close input in a finally path; preserve IOExceptions, wrap other parsing failures in IOException and tolerate a close IOException.

Available context and dependencies:
The surrounding project supplies REGION_TAG, Region and parseRegionElement(Element, boolean). Standard DOM/JAXP and collection APIs are available. The endpointVerification flag is forwarded to the helper; its validation behavior is not given.
