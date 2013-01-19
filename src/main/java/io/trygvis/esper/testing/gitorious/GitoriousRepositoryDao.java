package io.trygvis.esper.testing.gitorious;

import fj.data.*;
import io.trygvis.esper.testing.*;
import io.trygvis.esper.testing.util.sql.*;

import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.List;

import static io.trygvis.esper.testing.Util.*;

public class GitoriousRepositoryDao {
    private final Connection c;

    public GitoriousRepositoryDao(Connection c) {
        this.c = c;
    }

    private static final String ALL_FIELDS = "project_slug, name, atom_feed, last_update, last_successful_update";

    private List<GitoriousRepositoryDto> executeQuery(PreparedStatement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery()) {
            List<GitoriousRepositoryDto> list = new ArrayList<>();

            while (rs.next()) {
                list.add(new GitoriousRepositoryDto(
                    rs.getString(1),
                    rs.getString(2),
                    URI.create(rs.getString(3)),
                    Option.fromNull(rs.getTimestamp(4)).map(timestampToDate),
                    Option.fromNull(rs.getTimestamp(5)).map(timestampToDate)));
            }

            return list;
        }
    }

    public int countRepositories(String projectSlug, String name) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT count(*) FROM gitorious_repository WHERE project_slug=? AND name=?")) {
            s.setString(1, projectSlug);
            s.setString(2, name);
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<GitoriousRepositoryDto> selectForProject(String projectSlug) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + ALL_FIELDS + " FROM gitorious_repository WHERE project_slug=?")) {
            s.setString(1, projectSlug);
            return executeQuery(s);
        }
    }

    public List<GitoriousRepositoryDto> select(Boolean asc) throws SQLException {
        String sql = "SELECT " + ALL_FIELDS + " FROM gitorious_repository ";

        if(asc != null) {
            String[] orderBy = asc ? new String[]{"project_slug", "name"} : new String[]{"project_slug-", "name-"};
            sql += orderBy(orderBy, "project_slug", "name");
        }

        try (PreparedStatement s = c.prepareStatement(sql)) {
            return executeQuery(s);
        }
    }

    public void insertRepository(String projectSlug, String name, URI atomFeed) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO gitorious_repository(project_slug, name, atom_feed) VALUES(?, ?, ?)")) {
            s.setString(1, projectSlug);
            s.setString(2, name);
            s.setString(3, atomFeed.toASCIIString());
            s.executeUpdate();
        }
    }

    public void delete(GitoriousRepositoryDto repository) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("DELETE FROM gitorious_repository WHERE project_slug=? and name=?")) {
            s.setString(1, repository.projectSlug);
            s.setString(2, repository.name);
            s.executeUpdate();
        }
    }

    public void deleteForProject(String project) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("DELETE FROM gitorious_repository WHERE project_slug=?")) {
            s.setString(1, project);
            s.executeUpdate();
        }
    }

    public void updateTimestamp(String projectName, String slug, Timestamp lastUpdate, Option<Date> lastSuccessfulUpdate) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE gitorious_repository SET last_update=?, last_successful_update=? WHERE project_slug=? AND name=?")) {
            s.setTimestamp(1, lastUpdate);
            s.setTimestamp(2, lastSuccessfulUpdate.map(dateToTimestamp).toNull());
            s.setString(3, slug);
            s.setString(4, projectName);
            s.executeUpdate();
        }
    }
}
