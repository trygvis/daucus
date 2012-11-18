package io.trygvis.esper.testing.task;

import java.sql.*;
import java.util.*;

public class TaskDao {
    private final Connection c;
    private final String table;

    public TaskDao(Connection c, String table) {
        this.c = c;
        this.table = table;
    }

    public List<String> findTasks() throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT task_id FROM ? FOR UPDATE")) {
            s.setString(1, table);
            ResultSet rs = s.executeQuery();
            List<String> list = new ArrayList<>();
            while(rs.next()) {
                list.add(rs.getString(1));
            }
            return list;
        }
    }
}
