import java.util.List;

private List<OraclePackage> getAllRealOraclePackage(OBridgeConfiguration c) {
    String sql = "SELECT object_name FROM user_objects "
            + "WHERE object_type = 'PACKAGE' AND object_name LIKE ?";

    return jdbcTemplate.query(sql, (rs, rowNum) -> {
        String name = rs.getString("object_name");
        OraclePackage oraclePackage = new OraclePackage();
        oraclePackage.setName(name);
        oraclePackage.setProcedureList(getAllProcedure(name, ""));
        return oraclePackage;
    }, c.getPackagesLike());
}
