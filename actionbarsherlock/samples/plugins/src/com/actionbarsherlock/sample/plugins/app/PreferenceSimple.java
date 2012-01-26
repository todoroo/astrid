/*
 * Copyright (C) 2011 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.actionbarsherlock.sample.plugins.app;

import android.os.Bundle;
import android.support.v4.app.SherlockPreferenceActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;

import com.actionbarsherlock.sample.plugins.R;

public class PreferenceSimple extends SherlockPreferenceActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Save")
            .setIcon(R.drawable.ic_compose)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        menu.add("Search")
            .setIcon(R.drawable.ic_search)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        menu.add("Refresh")
            .setIcon(R.drawable.ic_refresh)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        return super.onCreateOptionsMenu(menu);
    }

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.preferences);
    }
}
