private boolean schemaExists(Connection connection, String schema) throws SQLException {
    String sql = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, schema);
      ResultSet resultSet = statement.executeQuery();
      if (resultSet.next()) {
        return resultSet.getBoolean(1);
      }
      return false;
    }
  }