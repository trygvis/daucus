package io.trygvis.esper.testing.gitorious;

import fj.data.*;
import io.trygvis.esper.testing.*;

import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.List;

public class GitoriousRepositoryDao extends Dao {
    public GitoriousRepositoryDao(Connection c) throws SQLException {
        super(c);
    }

    private static final String ALL_FIELDS = "project_slug, name, atom_feed, last_update, last_successful_update";

    private List<GitoriousRepository> executeQuery(PreparedStatement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery()) {
            List<GitoriousRepository> list = new ArrayList<>();

            while (rs.next()) {
                list.add(new GitoriousRepository(
                    rs.getString(1),
                    rs.getString(2),
                    URI.create(rs.getString(3)),
                    Option.fromNull(rs.getTimestamp(4)).map(timestampToDate),
                    Option.fromNull(rs.getTimestamp(5)).map(timestampToDate)));
            }

            return list;
        }
    }

    private final PreparedStatement countRepositories = prepareStatement("SELECT count(*) FROM gitorious_repository WHERE project_slug=? and name=?");

    public int countRepositories(String projectSlug, String name) throws SQLException {
        countRepositories.setString(1, projectSlug);
        countRepositories.setString(2, name);
        try (ResultSet rs = countRepositories.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private final PreparedStatement selectForProject = prepareStatement("SELECT " + ALL_FIELDS + " FROM gitorious_repository WHERE project_slug=?");

    public List<GitoriousRepository> selectForProject(String projectSlug) throws SQLException {
        selectForProject.setString(1, projectSlug);
        return executeQuery(selectForProject);
    }

    private final PreparedStatement select = prepareStatement("SELECT " + ALL_FIELDS + " FROM gitorious_repository");

    public List<GitoriousRepository> select() throws SQLException {
        return executeQuery(select);
    }

    private final PreparedStatement insertRepository = prepareStatement("INSERT INTO gitorious_repository(project_slug, name, atom_feed) VALUES(?, ?, ?)");

    public void insertRepository(String projectSlug, String name, URI atomFeed) throws SQLException {
        insertRepository.setString(1, projectSlug);
        insertRepository.setString(2, name);
        insertRepository.setString(3, atomFeed.toASCIIString());
        insertRepository.executeUpdate();
    }

    private final PreparedStatement delete = prepareStatement("DELETE FROM gitorious_repository WHERE project_slug=? and name=?");

    public void delete(GitoriousRepository repository) throws SQLException {
        delete.setString(1, repository.projectSlug);
        delete.setString(2, repository.name);
        delete.executeUpdate();
    }

    private final PreparedStatement deleteForProject = prepareStatement("DELETE FROM gitorious_repository WHERE project_slug=?");

    public void deleteForProject(String project) throws SQLException {
        deleteForProject.setString(1, project);
        deleteForProject.executeUpdate();
    }

    private final PreparedStatement updateTimestamp = prepareStatement("UPDATE gitorious_repository SET last_update=?, last_successful_update=? WHERE project_slug=? AND name=?");

    public void updateTimestamp(String projectName, String slug, Timestamp lastUpdate, Option<Date> lastSuccessfulUpdate) throws SQLException {
        updateTimestamp.setTimestamp(1, lastUpdate);
        updateTimestamp.setTimestamp(2, lastSuccessfulUpdate.map(dateToTimestamp).toNull());
        updateTimestamp.setString(3, slug);
        updateTimestamp.setString(4, projectName);
        updateTimestamp.executeUpdate();
    }
}
