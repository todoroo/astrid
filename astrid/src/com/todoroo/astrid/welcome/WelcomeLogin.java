package com.todoroo.astrid.welcome;

import org.json.JSONObject;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.facebook.android.AuthListener;
import com.timsu.astrid.C2DMReceiver;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncProvider;
import com.todoroo.astrid.activity.Eula;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StartupService;

public class WelcomeLogin extends ActFmLoginActivity implements AuthListener {

    // --- ui initialization

    public static final String KEY_SHOWED_WELCOME_LOGIN = "key_showed_welcome_login"; //$NON-NLS-1$

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    protected int getContentViewResource() {
        return R.layout.welcome_login_activity;
    }

    @Override
    protected int getTitleResource() {
        return R.string.welcome_login_title;
    }

    @SuppressWarnings("nls")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new StartupService().onStartupApplication(this);
        ContextManager.setContext(this);

        if (Preferences.getBoolean(KEY_SHOWED_WELCOME_LOGIN, false)) {
            finishAndShowNext();
        }
        initializeUI();
    }

    private void finishAndShowNext() {
        Intent welcomeScreen = new Intent(this, WelcomeScreen.class);
        startActivity(welcomeScreen);
        finish();
        Preferences.setBoolean(KEY_SHOWED_WELCOME_LOGIN, true);
    }

    @Override
    protected void initializeUI() {
        findViewById(R.id.gg_login).setOnClickListener(googleListener);
        setupTermsOfService();
        setupPWLogin();
        setupLoginLater();
    }

    private SpannableString getLinkString(String base, String linkComponent, final OnClickListener listener) {
        SpannableString link = new SpannableString (String.format("%s %s", //$NON-NLS-1$
                base, linkComponent));
        ClickableSpan linkSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                listener.onClick(widget);
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setUnderlineText(true);
                ds.setColor(Color.rgb(255, 96, 0));
            }
        };
        link.setSpan(linkSpan, base.length() + 1, link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return link;
    }

    private void setupTermsOfService() {
        TextView tos = (TextView)findViewById(R.id.tos);
        tos.setOnClickListener(showTosListener);

        String tosBase = getString(R.string.welcome_login_tos_base);
        String tosLink = getString(R.string.welcome_login_tos_link);
        SpannableString link = getLinkString(tosBase, tosLink, showTosListener);
        tos.setText(link);
    }

    private void setupPWLogin() {
        TextView pwLogin = (TextView) findViewById(R.id.pw_login);
        pwLogin.setOnClickListener(signUpListener);

        String pwLoginBase = getString(R.string.actfm_ALA_pw_login);
        String pwLoginLink = getString(R.string.actfm_ALA_pw_link);

        SpannableString link = getLinkString(pwLoginBase, pwLoginLink, signUpListener);
        pwLogin.setText(link);
    }

    private void setupLoginLater() {
        TextView loginLater = (TextView)findViewById(R.id.login_later);
        loginLater.setOnClickListener(loginLaterListener);
        String loginLaterBase = getString(R.string.welcome_login_later);
        SpannableString loginLaterLink = new SpannableString(String.format("%s", loginLaterBase)); //$NON-NLS-1$
        ClickableSpan laterSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                loginLaterListener.onClick(widget);
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setUnderlineText(true);
                ds.setColor(Color.rgb(255, 96, 0));
            }
        };
        loginLaterLink.setSpan(laterSpan, 0, loginLaterBase.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginLater.setText(loginLaterLink);
    }

    // --- event handler

    private final OnClickListener showTosListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Eula.showEulaBasic(WelcomeLogin.this);
        }
    };

    private final OnClickListener loginLaterListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            String title = getString(R.string.welcome_login_confirm_later_title);
            String confirmLater = getString(R.string.welcome_login_confirm_later_dialog);
            DialogUtilities.okCancelDialog(WelcomeLogin.this, title, confirmLater, confirmLaterListener, null);
        }

        private final DialogInterface.OnClickListener confirmLaterListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAndShowNext();
            }
        };
    };

    @Override
    @SuppressWarnings("nls")
    protected void postAuthenticate(JSONObject result, String token) {
        actFmPreferenceService.setToken(token);

        Preferences.setLong(ActFmPreferenceService.PREF_USER_ID,
                result.optLong("id"));
        Preferences.setString(ActFmPreferenceService.PREF_NAME, result.optString("name"));
        Preferences.setString(ActFmPreferenceService.PREF_EMAIL, result.optString("email"));
        Preferences.setString(ActFmPreferenceService.PREF_PICTURE, result.optString("picture"));

        setResult(RESULT_OK);

        // Delete the "Setup sync" task on successful login
        taskService.deleteWhere(Task.TITLE.eq(getString(R.string.intro_task_3_summary)));
        finishAndShowNext();

        if(!noSync) {
            new ActFmSyncProvider().synchronize(this);
        }

        try {
            C2DMReceiver.register();
        } catch (Exception e) {
            // phone may not support c2dm
            exceptionService.reportError("error-c2dm-register", e);
        }
    }
}
