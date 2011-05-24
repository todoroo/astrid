package com.timsu.astrid;

import java.io.IOException;

import org.json.JSONException;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class C2DMReceiver extends BroadcastReceiver {

    public static final String C2DM_SENDER = "c2dm@astrid.com"; //$NON-NLS-1$

    private static final String PREF_REGISTRATION = "c2dm_key";

    @Autowired ActFmSyncService actFmSyncService;
    @Autowired TagDataService tagDataService;

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
            handleRegistration(intent);
        } else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
            handleMessage(intent);
         }
     }

    /** Handle message */
    private void handleMessage(Intent intent) {
        String message = intent.getStringExtra("alert");
        Context context = ContextManager.getContext();

        Intent notifyIntent;
        if(intent.hasExtra("tag_id")) {
            TodorooCursor<TagData> cursor = tagDataService.query(
                    Query.select(TagData.ID).where(TagData.REMOTE_ID.eq(
                            intent.getLongExtra("tag_id", -1))));
            try {
                final TagData tagData = new TagData();
                if(cursor.getCount() == 0) {
                    tagData.setValue(TagData.NAME, intent.getStringExtra("title"));
                    tagData.setValue(TagData.REMOTE_ID, intent.getLongExtra("tag_id", 0));
                    tagDataService.save(tagData);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                actFmSyncService.fetchTag(tagData);
                            } catch (IOException e) {
                                Log.e("astrid-c2dm", "error fetching", e);
                            } catch (JSONException e) {
                                Log.e("astrid-c2dm", "error fetching", e);
                            }
                        }
                    }).start();
                } else {
                    cursor.moveToNext();
                    tagData.readFromCursor(cursor);
                }

                Filter filter= TagFilterExposer.filterFromTagData(context, tagData);
                notifyIntent = ShortcutActivity.createIntent(filter);
            } finally {
                cursor.close();
            }
        } else {
            notifyIntent = ShortcutActivity.createIntent(CoreFilterExposer.buildInboxFilter(context.getResources()));
        }

        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                Constants.NOTIFICATION_ACTFM, notifyIntent, 0);

        // create notification
        NotificationManager nm = new AndroidNotificationManager(ContextManager.getContext());
        Notification notification = new Notification(R.drawable.notif_pink_alarm,
                message, System.currentTimeMillis());
        String title = ContextManager.getString(R.string.app_name);
        if(intent.hasExtra("title"))
            title += ": " + intent.getStringExtra("title");
        notification.setLatestEventInfo(ContextManager.getContext(), title,
                message, pendingIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if("true".equals(intent.getStringExtra("sound")))
                notification.flags |= Notification.DEFAULT_SOUND;

        nm.notify(Constants.NOTIFICATION_ACTFM, notification);
    }

    private void handleRegistration(Intent intent) {
        String registration = intent.getStringExtra("registration_id");
        if (intent.getStringExtra("error") != null) {
            Log.w("astrid-actfm", "error-c2dm: " + intent.getStringExtra("error"));
        } else if (intent.getStringExtra("unregistered") != null) {
            // un-registration done
        } else if (registration != null) {
            try {
                DependencyInjectionService.getInstance().inject(this);
                actFmSyncService.invoke("user_set_c2dm", "c2dm", registration);
                Preferences.setString(PREF_REGISTRATION, registration);
            } catch (IOException e) {
                Log.e("astrid-actfm", "error-c2dm-transfer", e);
            }
        }
    }

    /** try to request registration from c2dm service */
    public static void register() {
        if(Preferences.getStringValue(PREF_REGISTRATION) != null)
            return;

        Context context = ContextManager.getContext();
        Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
        registrationIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0)); // boilerplate
        registrationIntent.putExtra("sender", C2DM_SENDER);
        context.startService(registrationIntent);
    }

    /** unregister with c2dm service */
    public static void unregister() {
        Preferences.setString(PREF_REGISTRATION, null);
        Context context = ContextManager.getContext();
        Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
        unregIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        context.startService(unregIntent);
    }

}
