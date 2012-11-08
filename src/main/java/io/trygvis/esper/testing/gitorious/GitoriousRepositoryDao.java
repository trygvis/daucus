package io.trygvis.esper.testing.gitorious;

import java.net.*;
import java.sql.*;
import java.util.*;

public class GitoriousRepositoryDao extends Dao {
    public GitoriousRepositoryDao(Connection c) throws SQLException {
        super(c);
    }

    private final PreparedStatement countRepositories = prepareStatement("SELECT count(*) FROM gitorious_repository WHERE project_slug=? and name=?");
    public int countRepositories(GitoriousRepository repository) throws SQLException {
        countRepositories.setString(1, repository.projectSlug);
        countRepositories.setString(2, repository.name);
        try (ResultSet rs = countRepositories.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private final PreparedStatement selectForProject = prepareStatement("SELECT project_slug, name, atom_feed FROM gitorious_repository WHERE project_slug=?");
    public List<GitoriousRepository> selectForProject(String projectSlug) throws Exception {
        selectForProject.setString(1, projectSlug);
        return executeQuery(selectForProject);
    }

    private final PreparedStatement selectAll = prepareStatement("SELECT project_slug, name, atom_feed FROM gitorious_repository");
    public List<GitoriousRepository> selectAll() throws Exception {
        return executeQuery(selectAll);
    }

    private final PreparedStatement insertRepository = prepareStatement("INSERT INTO gitorious_repository(project_slug, name, atom_feed) VALUES(?, ?, ?)");
    public void insertRepository(GitoriousRepository repository) throws SQLException {
        insertRepository.setString(1, repository.projectSlug);
        insertRepository.setString(2, repository.name);
        insertRepository.setString(3, repository.atom.toASCIIString());
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

    private List<GitoriousRepository> executeQuery(PreparedStatement statement) throws SQLException, URISyntaxException {
        try (ResultSet rs = statement.executeQuery()) {
            List<GitoriousRepository> list = new ArrayList<>();

            while(rs.next()) {
                list.add(new GitoriousRepository(
                    rs.getString(1),
                    rs.getString(2),
                    new URI(rs.getString(3))
                ));
            }

            return list;
        }
    }
}
