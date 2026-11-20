Task: task-03

Signature:
private boolean schemaExists(Connection connection, String schema) throws SQLException

Functional requirements:
Check whether the given schema name exists in PostgreSQL information_schema.schemata using an already-open JDBC connection. Return the boolean result from the first result row, or false if no result row is available. Close statement resources, keep the supplied connection open and propagate SQLExceptions.

Available context and dependencies:
Java java.sql.Connection, Statement/PreparedStatement, ResultSet and SQLException are available. Implement only this overload, not tenant initialization or other methods in the enclosing service.
