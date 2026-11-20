Task: quick-08

Signature:
public static void listen(final Consumer<String[]> consumer) throws Exception

Functional requirements:
Start a background TCP listener on the existing PORT constant. Accept connections repeatedly, receive a Java-serialized String[] from each connection and deliver it to consumer. Close each accepted socket and its input stream. Handle per-connection I/O and class-loading failures through the existing diagnostic helper so the listener can continue.

Available context and dependencies:
Java ServerSocket, Socket, object serialization, Consumer<String[]> and Thread APIs are available. PORT and ExceptionUtil.printStackTrace(Exception) are supplied by the surrounding project. The excerpt does not provide a shutdown API; do not invent its project contract.
