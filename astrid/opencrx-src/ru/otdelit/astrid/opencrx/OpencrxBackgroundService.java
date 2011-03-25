package ru.otdelit.astrid.opencrx;


import ru.otdelit.astrid.opencrx.sync.OpencrxSyncProvider;

import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncBackgroundService;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * SynchronizationService is the service that performs Astrid's background
 * synchronization with online task managers. Starting this service
 * schedules a repeating alarm which handles the synchronization
 *
 * @author Tim Su
 *
 */
public class OpencrxBackgroundService extends SyncBackgroundService {

    @Override
    protected SyncProvider<?> getSyncProvider() {
        return new OpencrxSyncProvider();
    }

    @Override
    protected SyncProviderUtilities getSyncUtilities() {
        return OpencrxUtilities.INSTANCE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StatisticsService.sessionStart(this);
    }

    @Override
    public void onDestroy() {
        StatisticsService.sessionStop(this);
        super.onDestroy();
    }

}
