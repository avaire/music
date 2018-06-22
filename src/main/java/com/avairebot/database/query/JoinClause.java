package com.avairebot.database.query;

import com.avairebot.contracts.database.QueryClause;

import java.util.ArrayList;
import java.util.List;

public class JoinClause implements QueryClause {
    /**
     * The type of join being performed.
     */
    public String type;

    /**
     * The table the join clause is joining to.
     */
    public String table;

    /**
     * The "on" clauses for the join.
     */
    public List<Clause> clauses = new ArrayList<>();

    /**
     * Create a new join clause instance.
     *
     * @param type  The type of the join.
     * @param table The table to join.
     */
    public JoinClause(String type, String table) {
        this.type = type;
        this.table = table;
    }

    /**
     * Adds a comparator to the join clause using the equal operator, if you want
     * to use a custom operator you can use {@link #on(java.lang.String, java.lang.String, java.lang.String) }
     *
     * @param one The first field to use in the join clause
     * @param two The second field to use in the join clause
     * @return The join clause instance.
     */
    public JoinClause on(String one, String two) {
        return on(one, "=", two);
    }

    /**
     * Adds a comparator to the join clause using the provided operator, if you want
     * to use the equal operator you can use {@link #on(java.lang.String, java.lang.String) }
     *
     * @param one      The first field to use in the join clause
     * @param operator The operator to compare the fields with
     * @param two      The second field to use in the join clause
     * @return The join clause instance.
     */
    public JoinClause on(String one, String operator, String two) {
        clauses.add(new Clause(one, operator, two));

        return this;
    }
}
