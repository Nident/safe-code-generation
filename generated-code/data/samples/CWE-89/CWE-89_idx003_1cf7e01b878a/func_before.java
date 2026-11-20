public ArrayList<String> getStudentAbsences(String studentID, String fromData, String toData) {
                ArrayList<String> absences = new ArrayList<String>();
                try {
                        stmt = c.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT pl.data,p.nr_lekcji from nieobecnosci n join przeprowadzone_lekcje pl on n.id_lekcji=pl.id join plan_lekcji p on pl.id_lekcji=p.id where id_ucznia='" + studentID + "' and data>=to_date('" + fromData + "', 'DD.MM.YYYY') and data<=to_date('" + toData + "', 'DD.MM.YYYY');");
                        while (rs.next()) {
                                String date = rs.getDate("data").toString();
                                int lesson = rs.getInt("nr_lekcji");

                                absences.add(date + " lekcja nr: " + lesson);
                        }

                        rs.close();
                        stmt.close();
                } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(e.getClass().getName() + ": " + e.getMessage());
                }
                return absences;
        }