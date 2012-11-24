package io.trygvis.esper.testing.gitorious;

import java.sql.*;
import java.util.*;

public class GitoriousProjectDao {
    private final Connection c;

    public GitoriousProjectDao(Connection c) {
        this.c = c;
    }

    public int countProjects(String slug) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT count(*) FROM gitorious_project WHERE slug=?")) {
            s.setString(1, slug);
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void insertProject(String slug) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO gitorious_project(slug) VALUES(?)")) {
            s.setString(1, slug);
            s.executeUpdate();
        }
    }

    public List<String> selectSlugs() throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT slug FROM gitorious_project")) {
            try (ResultSet rs = s.executeQuery()) {
                List<String> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
                return list;
            }
        }
    }

    public void delete(String slug) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("DELETE FROM gitorious_project WHERE slug=?")) {
            s.setString(1, slug);
            s.executeUpdate();
        }
    }
}
