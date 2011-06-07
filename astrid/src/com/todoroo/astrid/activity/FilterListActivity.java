/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.astrid.fragment.FilterListFragment;

/**
 * Activity that displays a user's task lists and allows users
 * to filter their task list.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterListActivity extends Activity {
    protected FilterListFragment filterlistFragment = null;

    public FilterListFragment getFilterlistFragment() {
        return filterlistFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.filter_list_fragment);
        if (filterlistFragment == null)
            filterlistFragment = (FilterListFragment)getFragmentManager().findFragmentById(R.id.filterlist_fragment);

        getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background));
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        if (filterlistFragment == null)
            filterlistFragment = (FilterListFragment)getFragmentManager().findFragmentById(R.id.filterlist_fragment);
        filterlistFragment.onContentChanged();
    }
}
