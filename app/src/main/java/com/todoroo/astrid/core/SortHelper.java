/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.isVisible;

/**
 * Helpers for sorting a list of tasks
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SortHelper {

    public static final int SORT_AUTO = 0;
    public static final int SORT_ALPHA = 1;
    public static final int SORT_DUE = 2;
    public static final int SORT_IMPORTANCE = 3;
    public static final int SORT_MODIFIED = 4;
    public static final int SORT_WIDGET = 5;

    private static final Order ORDER_TITLE = Order.asc(Functions.upper(Task.TITLE));

    /**
     * Takes a SQL query, and if there isn't already an order, creates an order.
     */
    public static String adjustQueryForFlagsAndSort(Preferences preferences, String originalSql, int sort) {
        // sort
        if(originalSql == null) {
            originalSql = "";
        }
        if(!originalSql.toUpperCase().contains("ORDER BY")) {
            Order order = orderForSortType(sort);

            if (preferences.getBoolean(R.string.p_reverse_sort, false)) {
                order = order.reverse();
            }
            originalSql += " ORDER BY " + order;
        }

        // flags
        if (preferences.getBoolean(R.string.p_show_completed_tasks, false)) {
            originalSql = originalSql.replace(Task.COMPLETION_DATE.eq(0).toString(),
                    Criterion.all.toString());
        } else {
            originalSql = originalSql.replace(Task.COMPLETION_DATE.eq(0).toString(),
                    Criterion.or(Task.COMPLETION_DATE.lte(0), Task.COMPLETION_DATE.gt(DateUtilities.now() - 60000)).toString());
        }
        if (preferences.getBoolean(R.string.p_show_hidden_tasks, false)) {
            originalSql = originalSql.replace(isVisible().toString(),
                    Criterion.all.toString());
        }

        return originalSql;
    }

    private static Order orderForSortType(int sortType) {
        Order order;
        switch(sortType) {
        case SORT_ALPHA:
            order = ORDER_TITLE;
            break;
        case SORT_DUE:
            order = Order.asc(Functions.caseStatement(Task.DUE_DATE.eq(0),
                    Functions.now()  + "*2", adjustedDueDateFunction()) + "+" + Task.IMPORTANCE);
            break;
        case SORT_IMPORTANCE:
            order = Order.asc(Task.IMPORTANCE + "*" + (2*DateUtilities.now()) +
                    "+" + Functions.caseStatement(Task.DUE_DATE.eq(0), 2 * DateUtilities.now(), Task.DUE_DATE));
            break;
        case SORT_MODIFIED:
            order = Order.desc(Task.MODIFICATION_DATE);
            break;
        case SORT_WIDGET:
        default:
            order = defaultTaskOrder();
        }
        if (sortType != SORT_ALPHA) {
            order.addSecondaryExpression(ORDER_TITLE);
        }

        return order;
    }

    /**
     * Returns SQL task ordering that is astrid's default algorithm
     */
    public static Order defaultTaskOrder() {
        return Order.asc(Functions.caseStatement(Task.DUE_DATE.eq(0),
                Functions.now() + "*2",
                adjustedDueDateFunction()) + " + " + (2 * DateUtilities.ONE_DAY) + " * " +
                Task.IMPORTANCE);
    }

    private static String adjustedDueDateFunction() {
        return "(CASE WHEN (" + Task.DUE_DATE.name + " / 60000) > 0" + " THEN " + Task.DUE_DATE.name + " ELSE " + "(" + Task.DUE_DATE.name + " + " + (DateUtilities.ONE_HOUR * 11 + DateUtilities.ONE_MINUTE * 59) + ") END)";
    }

}
