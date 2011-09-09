package com.todoroo.astrid.service.abtesting;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.todoroo.astrid.service.StatisticsConstants;

/**
 * Helper class to define options with their probabilities and descriptions
 * @author Sam Bosley <sam@astrid.com>
 *
 */
@SuppressWarnings({"nls", "unused"})
public class ABOptions {

    private static ABOptions instance = null;

    private ABOptions() { // Don't instantiate
        bundles = new HashMap<String, ABOptionBundle>();
        events = new HashMap<String, List<String>>();
        initialize();
    }

    /**
     * Gets the singleton instance of the ABOptions service.
     * @return
     */
    public synchronized static ABOptions getInstance() {
        if(instance == null)
            instance = new ABOptions();
        return instance;
    }

    /**
     * Gets the integer array of weighted probabilities for an option key
     * @param key
     * @return
     */
    public synchronized int[] getProbsForKey(String key) {
        if (bundles.containsKey(key)) {
            ABOptionBundle bundle = bundles.get(key);
            return bundle.weightedProbs;
        } else {
            return null;
        }
    }

    /**
     * Updates the weighted probability array for a given key. Returns true
     * on success, false if they key doesn't exist or if the array is the wrong
     * length.
     * @param key
     * @param newProbs
     * @return
     */
    public synchronized boolean setProbsForKey(String key, int[] newProbs) {
        if (bundles.containsKey(newProbs)) {
            ABOptionBundle bundle = bundles.get(key);
            if (bundle.descriptions == null || newProbs.length == bundle.descriptions.length) {
                bundle.weightedProbs = newProbs;
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the string array of option descriptions for an option key
     * @param key
     * @return
     */
    public String[] getDescriptionsForKey(String key) {
        if (bundles.containsKey(key)) {
            ABOptionBundle bundle = bundles.get(key);
            return bundle.descriptions;
        } else {
            return null;
        }
    }

    /**
     * Returns the description for a particular choice of the given option
     * @param key
     * @param optionIndex
     * @return
     */
    public String getDescriptionForOption(String key, int optionIndex) {
        if (bundles.containsKey(key)) {
            ABOptionBundle bundle = bundles.get(key);
            if (bundle.descriptions != null && optionIndex < bundle.descriptions.length) {
                return bundle.descriptions[optionIndex];
            }
        }
        return null;
    }

    /**
     * Maps keys (i.e. preference key identifiers) to feature weights and descriptions
     */
    private final HashMap<String, ABOptionBundle> bundles;
    private final HashMap<String, List<String>> events; // maps events to lists of interested keys

    private static class ABOptionBundle {
        public int[] weightedProbs;
        public String[] descriptions;

        public ABOptionBundle(int[] weightedProbs, String[] descriptions) {
            this.weightedProbs = weightedProbs;
            this.descriptions = descriptions;
        }
    }

    private static final String PROBS_SUFFIX = "_PROBS";
    private static final String DESCRIPTIONS_SUFFIX = "_DESCRIPTIONS";
    private static final String EVENTS_SUFFIX = "_RELEVANT_EVENTS";

    private void initialize() { // Set up
        Class<?> abOptions = ABOptions.class;
        for(Field field : abOptions.getDeclaredFields()) {
            if (isOptionKeyField(field)) {
                try {
                    String key = (String) field.get(this);
                    Field probsField = abOptions.getDeclaredField(field.getName() + PROBS_SUFFIX);
                    int[] probs = (int[]) probsField.get(this);

                    Field descriptionsField;
                    String[] descriptions;
                    try {
                        descriptionsField = abOptions.getDeclaredField(field.getName() + DESCRIPTIONS_SUFFIX);
                        descriptions = (String[]) descriptionsField.get(this);
                    } catch (NoSuchFieldException e) {
                        descriptions = null;
                    }

                    Field relevantEventsField;
                    String[] relevantEvents = null;
                    if (descriptions != null) { // Can't tag options with no descriptions, so events are irrelevant
                        try {
                            relevantEventsField = abOptions.getDeclaredField(field.getName() + EVENTS_SUFFIX);
                            relevantEvents = (String[]) relevantEventsField.get(this);
                        } catch (NoSuchFieldException e) {
                            // Do nothing, already null
                        }
                    }
                    if (relevantEvents != null) {
                        for (String curr : relevantEvents) {
                            List<String> interestedKeys = events.get(curr);
                            if (interestedKeys == null) {
                                interestedKeys = new ArrayList<String>();
                                events.put(curr, interestedKeys);
                            }
                            interestedKeys.add(key);
                        }
                    }

                    ABOptionBundle newBundle = new ABOptionBundle(probs, descriptions);
                    bundles.put(key, newBundle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isOptionKeyField(Field field) {
        return !field.getName().endsWith(PROBS_SUFFIX) && !field.getName().endsWith(DESCRIPTIONS_SUFFIX) && !field.getName().endsWith(EVENTS_SUFFIX)
                && Modifier.isStatic(field.getModifiers()) && !field.getName().equals("instance");
    }

    public boolean isValidKey(String key) {
        return bundles.containsKey(key);
    }

    /**
     * Gets a localytics attribute array for the specified event.
     * @param event
     * @return
     */
    public String[] getLocalyticsAttributeArrayForEvent(String event) {
        ArrayList<String> attributes = new ArrayList<String>();
        List<String> interestedKeys = events.get(event);
        if (interestedKeys != null)
            for (String key : interestedKeys) {
                // Get choice if exists and add to array
                if (isValidKey(key)) {
                    ABOptionBundle bundle = bundles.get(key);
                    int choice = ABChooser.getInstance().readChoiceForOption(key);
                    if (choice != ABChooser.NO_OPTION &&
                            bundle.descriptions != null && choice < bundle.descriptions.length) {
                        attributes.add(key);
                        attributes.add(getDescriptionForOption(key, choice));
                    }
                }
            }
        return attributes.toArray(new String[attributes.size()]);
    }


    /*
     * A/B testing options are defined below according to the following spec:
     *
     * public static String AB_OPTION_<NAME> = "<key>"
     * --This key is used to identify the option in the application and in the preferences
     *
     * private static int[] AB_OPTION_<NAME>_PROBS = { int, int, ... }
     * --The different choices in an option correspond to an index in the probability array.
     * Probabilities are expressed as integers to easily define relative weights. For example,
     * the array { 1, 2 } would mean option 0 would happen one time for every two occurrences of option 1
     *
     * (optional)
     * private static String[] AB_OPTION_<NAME>_DESCRIPTIONS = { "...", "...", ... }
     * --A string description of each option. Useful for tagging events. The index of
     * each description should correspond to the events location in the probability array
     * (i.e. the arrays should be the same length if this one exists)
     *
     * (optional)
     * private static String[] AB_OPTION_<NAME>_RELEVANT_EVENTS = { "...", "...", ... }
     * --An arbitrary length list of relevant localytics events. When events are
     * tagged from StatisticsService, they will be appended with attributes
     * that have that event in this array
     */

    public static String AB_OPTION_FIRST_ACTIVITY = "ab_first_activity";
    private static int[] AB_OPTION_FIRST_ACTIVITY_PROBS = { 1, 1 };
    private static String[] AB_OPTION_FIRST_ACTIVITY_DESCRIPTIONS = { "ab-show-tasks-first", "ab-show-lists-first" };
    private static String[] AB_OPTION_FIRST_ACTIVITY_RELEVANT_EVENTS = { StatisticsConstants.CREATE_TASK,
                                                                StatisticsConstants.TASK_CREATED_TASKLIST,
                                                                StatisticsConstants.ACTFM_LIST_SHARED,
                                                                StatisticsConstants.ACTFM_NEW_USER };

    public static String AB_OPTION_WELCOME_LOGIN = "ab_welcome_login";
    private static int[] AB_OPTION_WELCOME_LOGIN_PROBS = { 0, 1 };
    private static String[] AB_OPTION_WELCOME_LOGIN_DESCRIPTIONS = { "ab-welcome-login-show", "ab-welcome-login-skip" };
    private static String[] AB_OPTION_WELCOME_LOGIN_RELEVANT_EVENTS = { StatisticsConstants.ACTFM_NEW_USER };
}
