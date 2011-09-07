package com.todoroo.astrid.abtesting;

import java.util.Random;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.service.StatisticsService;

/**
 * Helper class to facilitate A/B testing by randomly choosing an option
 * based on probabilities that can be supplied from local defaults
 * @author Sam Bosley <sam@astrid.com>
 *
 */
public class ABChooser {

    private static ABChooser instance = null;
    private final ABOptions options;

    private ABChooser() {
        options = ABOptions.getInstance();
    }

    public static synchronized ABChooser getInstance() {
        if (instance == null)
            instance = new ABChooser();
        return instance;
    }

    /**
     * Retrieves the choice for the specified feature if already made,
     * or chooses one randomly from the distribution of feature probabilities
     * if not.
     *
     * The statistics session needs to be open here in order to collect statistics
     *
     * @param optionKey - the preference key string of the option (defined in ABOptions)
     * @return
     */
    public int getChoiceForOption(String optionKey) {
        int pref = Preferences.getInt(optionKey, -1);
        if (pref > -1) return pref;

        int chosen = -1;
        if (options.isValidKey(optionKey)) {
            int[] optionProbs = options.getProbsForKey(optionKey);
            chosen = chooseOption(optionProbs);
            setChoiceForOption(optionKey, chosen);

            StatisticsService.reportEvent(options.getDescriptionForOption(optionKey, chosen)); // Session should be open
        }
        return chosen;
    }

    /**
     * Changes the choice of an A/B feature in the preferences. Useful for
     * the feature flipper (can manually override previous choice)
     * @param optionKey
     * @param choiceIndex
     */
    public void setChoiceForOption(String optionKey, int choiceIndex) {
        if (options.isValidKey(optionKey))
            Preferences.setInt(optionKey, choiceIndex);
    }

    /*
     * Helper method to choose an option from an int[] corresponding to the
     * relative weights of each option. Returns the index of the chosen option.
     */
    private int chooseOption(int[] optionProbs) {
        int sum = 0;
        for (int opt : optionProbs) { // Compute sum
            sum += opt;
        }

        double rand = new Random().nextDouble() * sum; // Get uniformly distributed double between [0, sum)
        sum = 0;
        for (int i = 0; i < optionProbs.length; i++) {
            sum += optionProbs[i];
            if (rand <= sum) return i;
        }
        return optionProbs.length - 1;
    }
}
