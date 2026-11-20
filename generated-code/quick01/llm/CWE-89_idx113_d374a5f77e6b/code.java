import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

private boolean schemaExists(Connection connection, String schema) throws SQLException {
    String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, schema);
        try (ResultSet result = statement.executeQuery()) {
            return result.next() && result.getBoolean(1);
        }
    }
}
