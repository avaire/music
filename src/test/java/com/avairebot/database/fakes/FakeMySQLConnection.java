package com.avairebot.database.fakes;

import com.avairebot.contracts.database.StatementInterface;
import com.avairebot.contracts.database.connections.FilenameDatabase;
import com.avairebot.database.DatabaseManager;
import com.avairebot.database.grammar.mysql.Create;
import com.avairebot.database.grammar.mysql.Delete;
import com.avairebot.database.grammar.mysql.Insert;
import com.avairebot.database.grammar.mysql.Update;
import com.avairebot.database.grammar.sqlite.Select;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.database.schema.Blueprint;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.Map;

public class FakeMySQLConnection extends FilenameDatabase {

    FakeMySQLConnection() {
        this.setFilename(":memory:");
    }

    @Override
    protected boolean initialize() {
        return false;
    }

    @Override
    protected void queryValidation(StatementInterface paramStatement) throws SQLException {

    }

    @Override
    public boolean open() throws SQLException {
        return false;
    }

    @Override
    public StatementInterface getStatement(String query) throws SQLException {
        return null;
    }

    @Override
    public boolean hasTable(String table) {
        return false;
    }

    @Override
    public boolean truncate(String table) {
        return false;
    }

    @Override
    public String create(DatabaseManager manager, Blueprint blueprint, @Nonnull Map<String, Boolean> options) {
        return setupAndRun(new Create(), blueprint, manager, options);
    }

    @Override
    public String delete(DatabaseManager manager, QueryBuilder query, @Nullable Map<String, Boolean> options) {
        return setupAndRun(new Delete(), query, manager, options);
    }

    @Override
    public String insert(DatabaseManager manager, QueryBuilder query, @Nullable Map<String, Boolean> options) {
        return setupAndRun(new Insert(), query, manager, options);
    }

    @Override
    public String select(DatabaseManager manager, QueryBuilder query, @Nullable Map<String, Boolean> options) {
        return setupAndRun(new Select(), query, manager, options);
    }

    @Override
    public String update(DatabaseManager manager, QueryBuilder query, @Nullable Map<String, Boolean> options) {
        return setupAndRun(new Update(), query, manager, options);
    }
}
