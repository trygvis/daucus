package io.trygvis.esper.testing.gitorious;

import java.sql.*;
import java.util.*;

public class GitoriousProjectDao extends Dao {
    public GitoriousProjectDao(Connection c) throws SQLException {
        super(c);
    }

    private final PreparedStatement countProjects = prepareStatement("SELECT count(*) FROM gitorious_project WHERE slug=?");
    public int countProjects(String slug) throws SQLException {
        countProjects.setString(1, slug);
        try(ResultSet rs = countProjects.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private final PreparedStatement insertProject = prepareStatement("INSERT INTO gitorious_project(slug) VALUES(?)");
    public void insertProject(GitoriousProject project) throws SQLException {
        insertProject.setString(1, project.slug);
        insertProject.executeUpdate();
    }

    private final PreparedStatement selectAll = prepareStatement("SELECT slug FROM gitorious_project");
    public List<String> selectAll() throws SQLException {
        try (ResultSet rs = selectAll.executeQuery()) {
            List<String> list = new ArrayList<>();
            while(rs.next()) {
                list.add(rs.getString(1));
            }
            return list;
        }
    }

    private final PreparedStatement delete = prepareStatement("DELETE FROM gitorious_project WHERE slug=?");
    public void delete(String slug) throws SQLException {
        delete.setString(1, slug);
        delete.executeUpdate();
    }
}
