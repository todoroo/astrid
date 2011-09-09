package com.todoroo.astrid.abtesting;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

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

    private static class ABOptionBundle {
        public int[] weightedProbs;
        public String[] descriptions;

        public ABOptionBundle(int[] weightedProbs, String[] descriptions) {
            this.weightedProbs = weightedProbs;
            this.descriptions = descriptions;
        }
    }

    private void initialize() { // Set up
        Class<?> abOptions = ABOptions.class;
        for(Field field : abOptions.getDeclaredFields()) {
            if (isOptionKeyField(field)) {
                try {
                    String key = (String) field.get(this);
                    Field probsField = abOptions.getDeclaredField(field.getName() + "_PROBS");
                    int[] probs = (int[]) probsField.get(this);

                    Field descriptionsField;
                    String[] descriptions;
                    try {
                        descriptionsField = abOptions.getDeclaredField(field.getName() + "_DESCRIPTIONS");
                        descriptions = (String[]) descriptionsField.get(this);
                    } catch (NoSuchFieldException e) {
                        descriptions = null;
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
        return !field.getName().endsWith("_PROBS") && !field.getName().endsWith("_DESCRIPTIONS")
                && Modifier.isStatic(field.getModifiers()) && !field.getName().equals("instance");
    }

    public boolean isValidKey(String key) {
        return bundles.containsKey(key);
    }

    public String[] getLocalyticsAttributeArray() {
        ArrayList<String> attributes = new ArrayList<String>();
        for (String key : bundles.keySet()) {
            int choice = ABChooser.getInstance().readChoiceForOption(key);
            ABOptionBundle bundle = bundles.get(key);
            if (choice != ABChooser.NO_OPTION && choice < bundle.descriptions.length) {
                attributes.add(key);
                attributes.add(bundle.descriptions[choice]);
            }
        }
        return attributes.toArray(new String[attributes.size()]);
    }

    public static String AB_OPTION_FIRST_ACTIVITY = "ab_first_activity";
    private static int[] AB_OPTION_FIRST_ACTIVITY_PROBS = { 1, 1 };
    private static String[] AB_OPTION_FIRST_ACTIVITY_DESCRIPTIONS = { "ab-show-tasks-first", "ab-show-lists-first" };

    public static String AB_OPTION_WELCOME_LOGIN = "ab_welcome_login";
    private static int[] AB_OPTION_WELCOME_LOGIN_PROBS = { 0, 1 }; // Index 0 = show welcome login, index 1 = don't show welcome login

}
