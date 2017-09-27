/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

public final class Operator {

    private final String operator;
    public static final Operator eq = new Operator("=");
    public static final Operator neq = new Operator("<>");
    public static final Operator isNull = new Operator("IS NULL");
    public static final Operator isNotNull = new Operator("IS NOT NULL");
    public static final Operator gt = new Operator(">");
    public static final Operator lt = new Operator("<");
    public static final Operator lte = new Operator("<=");
    public static final Operator and = new Operator("AND");
    public static final Operator or = new Operator("OR");
    public static final Operator not = new Operator("NOT");
    public static final Operator exists = new Operator("EXISTS");
    public static final Operator like = new Operator("LIKE");
    public static final Operator in = new Operator("IN");

    private Operator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return this.operator;
    }
}
