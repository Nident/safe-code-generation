Task: quick-09

Signature:
public Object getResourceDataForKey(String key)

Functional requirements:
Find the data separator in a resource key using DATA_SEPARATOR_PATTERN. Decode the suffix as ISO-8859-1 bytes and pass it to the existing decrypt helper. If separator capture group 1 is B, return the decoded bytes; otherwise deserialize the decoded Java object. Return null when no separator matches or a handled decode/deserialization failure occurs, using existing project diagnostics.

Available context and dependencies:
The containing project supplies DATA_SEPARATOR_PATTERN, decrypt(byte[]), log and Messages message lookup/constants. Their actual implementations and accepted serialized types are not supplied. Java regex, byte-stream and serialization APIs are available; list unresolved contracts.
