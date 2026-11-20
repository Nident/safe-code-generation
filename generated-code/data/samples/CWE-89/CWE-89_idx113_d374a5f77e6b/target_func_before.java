private boolean schemaExists(Connection connection, String schema) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      String queryTemplate = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = '%s');";
      ResultSet resultSet = statement.executeQuery(String.format(queryTemplate, schema));
      if (resultSet.next()) {
        return resultSet.getBoolean(1);
      }
      return false;
    }
  }