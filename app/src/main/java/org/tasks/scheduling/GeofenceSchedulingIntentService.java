package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;

import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.jobs.JobManager;
import org.tasks.location.GeofenceService;

import javax.inject.Inject;

import timber.log.Timber;

public class GeofenceSchedulingIntentService extends InjectingJobIntentService {

    public static void enqueueWork(Context context) {
        JobIntentService.enqueueWork(context, GeofenceSchedulingIntentService.class, JobManager.JOB_ID_GEOFENCE_SCHEDULING, new Intent());
    }

    @Inject GeofenceService geofenceService;

    @Override
    protected void onHandleWork(Intent intent) {
        super.onHandleWork(intent);

        Timber.d("onHandleWork(%s)", intent);

        geofenceService.cancelGeofences();
        geofenceService.setupGeofences();
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
