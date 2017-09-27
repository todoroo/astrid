/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import java.util.ArrayList;

import static com.todoroo.andlib.sql.SqlConstants.COMMA;
import static com.todoroo.andlib.sql.SqlConstants.GROUP_BY;
import static com.todoroo.andlib.sql.SqlConstants.LIMIT;
import static com.todoroo.andlib.sql.SqlConstants.ORDER_BY;
import static com.todoroo.andlib.sql.SqlConstants.SPACE;
import static com.todoroo.andlib.sql.SqlConstants.WHERE;
import static java.util.Arrays.asList;

/**
 * Query Template returns a bunch of criteria that allows a query to be
 * constructed
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class QueryTemplate {

    private final ArrayList<Criterion> criterions = new ArrayList<>();
    private final ArrayList<Join> joins = new ArrayList<>();
    private final ArrayList<Field> groupBies = new ArrayList<>();
    private final ArrayList<Order> orders = new ArrayList<>();
    private Integer limit = null;

    public QueryTemplate join(Join... join) {
        joins.addAll(asList(join));
        return this;
    }

    public QueryTemplate where(Criterion criterion) {
        criterions.add(criterion);
        return this;
    }

    public QueryTemplate orderBy(Order... order) {
        orders.addAll(asList(order));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        visitJoinClause(sql);
        visitWhereClause(sql);
        visitGroupByClause(sql);
        visitOrderByClause(sql);
        if(limit != null) {
            sql.append(LIMIT).append(SPACE).append(limit);
        }
        return sql.toString();
    }

    private void visitOrderByClause(StringBuilder sql) {
        if (orders.isEmpty()) {
            return;
        }
        sql.append(ORDER_BY);
        for (Order order : orders) {
            sql.append(SPACE).append(order).append(COMMA);
        }
        sql.deleteCharAt(sql.length() - 1).append(SPACE);
    }

    private void visitGroupByClause(StringBuilder sql) {
        if (groupBies.isEmpty()) {
            return;
        }
        sql.append(GROUP_BY);
        for (Field groupBy : groupBies) {
            sql.append(SPACE).append(groupBy).append(COMMA);
        }
        sql.deleteCharAt(sql.length() - 1).append(SPACE);
    }

    private void visitWhereClause(StringBuilder sql) {
        if (criterions.isEmpty()) {
            return;
        }
        sql.append(WHERE);
        for (Criterion criterion : criterions) {
            sql.append(SPACE).append(criterion).append(SPACE);
        }
    }

    private void visitJoinClause(StringBuilder sql) {
        for (Join join : joins) {
            sql.append(join).append(SPACE);
        }
    }

    public QueryTemplate limit(int limitValue) {
        this.limit = limitValue;
        return this;
    }
}
