
package com.todoroo.astrid.welcome.tutorial;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.activity.Eula;
import com.todoroo.astrid.welcome.WelcomeLogin;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;

public class WelcomeWalkthrough extends ActFmLoginActivity {
    ViewPager mPager;
    ViewPagerAdapter mAdapter;
    PageIndicator mIndicator;
    View currentView;
    int currentPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.welcome_walkthrough);

        mAdapter = new ViewPagerAdapter(this);
        mAdapter.parent = this;

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
    }
    @Override
    protected int getContentViewResource() {
        return R.layout.welcome_walkthrough;
    }

    @Override
    protected int getTitleResource() {
        return 0;
    }
    public void pageScrolled(int position, View view){
        Log.d(null, "Updated ui");
        currentView = view;
        currentPage = position;
        if (position == mAdapter.getCount()-1) {
            initializeUI();
        }
    }



    @Override
    protected void initializeUI() {
        if(mAdapter == null)
            return;
        if(currentPage == mAdapter.getCount()-1){
            super.initializeUI();
            setupTermsOfService();
            setupLoginLater();
        }
    }

    protected SpannableString getLinkStringWithCustomInterval(String base, String linkComponent,
                                                            int start, int endOffset, final OnClickListener listener) {
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
                ds.setColor(Color.rgb(255, 255, 255));
            }
        };
        link.setSpan(linkSpan, start, link.length() + endOffset, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return link;
    }
    protected void setupTermsOfService() {
        TextView tos = (TextView)currentView.findViewById(R.id.tos);
        tos.setOnClickListener(showTosListener);

        String tosBase = getString(R.string.welcome_login_tos_base);
        String tosLink = getString(R.string.welcome_login_tos_link);
        SpannableString link = getLinkStringWithCustomInterval(tosBase, tosLink, tosBase.length() + 2, -1, showTosListener);
        tos.setText(link);
    }
    protected void setupPWLogin() {
        Button pwLogin = (Button) findViewById(R.id.pw_login);
        pwLogin.setOnClickListener(signUpListener);
    }
    /*
    protected void setupWalkthroughLogin() {
        Button loginButton = (Button)currentView.findViewById(R.id.walkthrough_login);
        loginButton.setOnClickListener(showWalkthroughLoginListener);
    }*/


    protected void setupLoginLater() {
        TextView loginLater = (TextView)currentView.findViewById(R.id.login_later);
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
                ds.setColor(Color.rgb(255, 255, 255));
            }
        };
        loginLaterLink.setSpan(laterSpan, 0, loginLaterBase.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginLater.setText(loginLaterLink);
    }

    protected final OnClickListener showTosListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Eula.showEulaBasic(WelcomeWalkthrough.this);
        }
    };
    private void showWelcomeLoginActivty() {
        Intent showWelcomeLogin = new Intent(this, WelcomeLogin.class);
        showWelcomeLogin.putExtra(ActFmLoginActivity.SHOW_TOAST, false);
        startActivity(showWelcomeLogin);
    }
    protected final OnClickListener showWalkthroughLoginListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showWelcomeLoginActivty();
        }
    };

    protected final OnClickListener loginLaterListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            String title = getString(R.string.welcome_login_confirm_later_title);
            String confirmLater = getString(R.string.welcome_login_confirm_later_dialog);
            DialogUtilities.okCancelCustomDialog(WelcomeWalkthrough.this, title, confirmLater,
                    R.string.welcome_login_confirm_later_ok,
                    R.string.welcome_login_confirm_later_cancel,
                    android.R.drawable.ic_dialog_alert,
                    null, confirmLaterListener);
        }

        private final DialogInterface.OnClickListener confirmLaterListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };
    };

}

