/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.Intent;
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
    public FilterListFragment getFilterlistFragment() {
        return (FilterListFragment)getFragmentManager().findFragmentById(R.id.filterlist_fragment);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.filter_list_fragment);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        if (getFilterlistFragment() != null)
            getFilterlistFragment().onContentChanged();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (getFilterlistFragment() != null)
            getFilterlistFragment().onNewIntent(intent);
    }

}
