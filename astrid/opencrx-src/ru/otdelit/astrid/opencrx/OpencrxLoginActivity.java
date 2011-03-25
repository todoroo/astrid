/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ru.otdelit.astrid.opencrx;

import ru.otdelit.astrid.opencrx.api.ApiAuthenticationException;
import ru.otdelit.astrid.opencrx.api.OpencrxInvoker;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.service.StatisticsService;

/**
 * This activity allows users to sign in or log in to Producteev
 *
 * @author arne.jans
 *
 */
public class OpencrxLoginActivity extends Activity {

    // --- ui initialization

    public OpencrxLoginActivity() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

        setContentView(R.layout.opencrx_login_activity);
        setTitle(R.string.opencrx_PLA_title);

        final TextView errors = (TextView) findViewById(R.id.error);
        final EditText emailEditText = (EditText) findViewById(R.id.email);
        final EditText passwordEditText = (EditText) findViewById(R.id.password);

        Button signIn = (Button) findViewById(R.id.signIn);
        signIn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                errors.setVisibility(View.GONE);

                Editable email = emailEditText.getText();
                Editable password = passwordEditText.getText();
                if(email.length() == 0 || password.length() == 0) {
                    errors.setVisibility(View.VISIBLE);
                    errors.setText(R.string.opencrx_PLA_errorEmpty);
                    return;
                }

                performLogin(email.toString(), password.toString());
            }

        });
    }


    private void performLogin(final String email, final String password) {
        final ProgressDialog dialog = DialogUtilities.progressDialog(this,
                getString(R.string.DLG_wait));
        final TextView errors = (TextView) findViewById(R.id.error);
        dialog.show();
        new Thread() {
            @Override
            public void run() {
                OpencrxInvoker invoker = new OpencrxInvoker();
                final StringBuilder errorMessage = new StringBuilder();
                try {
                    String host = Preferences.getStringValue(R.string.opencrx_PPr_host_key);
                    String segment = Preferences.getStringValue(R.string.opencrx_PPr_segment_key);
                    String provider = Preferences.getStringValue(R.string.opencrx_PPr_provider_key);

                    invoker.setOpencrxPreferences(host, segment, provider);
                    invoker.authenticate(email, password);

                    Preferences.setString(R.string.opencrx_PPr_login, email);
                    Preferences.setString(R.string.opencrx_PPr_password, password);
                    OpencrxUtilities.INSTANCE.setToken("token"); //$NON-NLS-1$

                    StatisticsService.reportEvent("opencrx-login"); //$NON-NLS-1$

                    synchronize();
                } catch (ApiAuthenticationException e) {
                    errorMessage.append(getString(R.string.opencrx_PLA_errorAuth));
                } catch (Exception e) {
                    errorMessage.append(e.getMessage());
                } finally {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            dialog.dismiss();
                            if(errorMessage.length() > 0) {
                                errors.setVisibility(View.VISIBLE);
                                errors.setText(errorMessage);
                            }
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * Perform synchronization
     */
    protected void synchronize() {
        startService(new Intent(null, null,
                this, OpencrxBackgroundService.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        StatisticsService.sessionStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatisticsService.sessionStop(this);
    }

}
