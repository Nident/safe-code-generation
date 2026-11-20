Task: task-02

Signature:
public static String sanitizeInput(String string)

Functional requirements:
Process an HTML-like input string for display by removing script elements, event-handler attributes and executable URL content while retaining ordinary text. Return the processed string.

Available context and dependencies:
The containing class is FormatUtil. Only a String input and String return value are specified; no HTML parser dependency is supplied by the source fragment. State any additional dependency needed by your implementation in dependency_notes.
