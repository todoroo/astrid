/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import java.util.ArrayList;
import java.util.List;

import static com.todoroo.andlib.sql.SqlConstants.SPACE;

public class Order {
    private final Object expression;
    private final List<Order> secondaryExpressions;
    private final OrderType orderType;

    private Order(Object expression) {
        this(expression, OrderType.ASC);
    }

    private Order(Object expression, OrderType orderType) {
        this.expression = expression;
        this.orderType = orderType;
        this.secondaryExpressions = new ArrayList<>();
    }

    public static Order asc(Object expression) {
        return new Order(expression);
    }

    public static Order desc(Object expression) {
        return new Order(expression, OrderType.DESC);
    }

    public void addSecondaryExpression(Order secondary) {
        secondaryExpressions.add(secondary);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(expression.toString())
          .append(SPACE)
          .append(orderType.toString());

        for (Order secondary : secondaryExpressions) {
            sb.append(", ").append(secondary.toString()); //$NON-NLS-1$
        }

        return sb.toString();
    }

    public Order reverse() {
        if(orderType == OrderType.ASC) {
            return new Order(expression, OrderType.DESC);
        } else {
            return new Order(expression, OrderType.ASC);
        }
    }
}
