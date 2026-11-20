Task: quick-02

Signature:
static public File allocateFile(File dir, String name)

Functional requirements:
Choose an unused file path below dir from name. Remove the query suffix when a question mark occurs after the first character. If the file already exists, append increasing numeric suffixes starting at -2 before the first extension. Create parent directories and return the selected File; do not create the file itself.

Available context and dependencies:
Java File APIs are available. No project-specific helpers are needed.
