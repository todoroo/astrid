package com.todoroo.astrid.utility;


public final class Constants {

    // --- general application constants

    /**
     * Flurry API Key
     */
    public static final String FLURRY_KEY = "T3JAY9TV2JFMJR4YTG16"; //$NON-NLS-1$

    /**
     * Application Package
     */
    public static final String PACKAGE = "com.timsu.astrid"; //$NON-NLS-1$

    /**
     * Whether this is an OEM installation
     */
    public static final boolean OEM = false;

    /**
     * Whether this is an Android Market-disabled build
     */
    public static final boolean MARKET_DISABLED = false;

    /**
     * Interval to update the widget (in order to detect hidden tasks
     * becoming visible)
     */
    public static final long WIDGET_UPDATE_INTERVAL = 30 * 60 * 1000L;

    /**
     * Whether to turn on debugging logging and UI
     */
    public static final boolean DEBUG = false;

    /**
     * Astrid Help URL
     */
    public static final String HELP_URL = "http://weloveastrid.com/help-user-guide-astrid-v3/active-tasks/"; //$NON-NLS-1$

    // --- notification id's

    /** Notification Manager id for sync notifications */
    public static final int NOTIFICATION_SYNC = -1;

    /** Notification Manager id for timing */
    public static final int NOTIFICATION_TIMER = -2;

    /** Notification Manager id for locale */
    public static final int NOTIFICATION_LOCALE = -3;

    /** Notification Manager id for producteev notifications*/
    public static final int NOTIFICATION_PRODUCTEEV_NOTIFICATIONS = -4;

    /** Notification Manager id for astrid.com */
    public static final int NOTIFICATION_ACTFM = -5;


}
