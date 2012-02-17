/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.peterkuterna.android.apps.pinnedheader;

import android.util.SparseIntArray;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;

/**
 * A {@link SectionIndexer} implementation on an array of {@link String} objects.
 * Based on the {@link AlphabetIndexer}.  
 */
public class StringArrayAlphabetIndexer implements SectionIndexer {
	
	/**
	 * The array of data
	 */
	protected String[] mArray;

    /**
     * The string of characters that make up the indexing sections.
     */
    protected CharSequence mAlphabet;

    /**
     * Cached length of the alphabet array.
     */
    private int mAlphabetLength;

    /**
     * This contains a cache of the computed indices so far. It will get reset whenever
     * the dataset changes or the cursor changes.
     */
    private SparseIntArray mAlphaMap;

    /**
     * Use a collator to compare strings in a localized manner.
     */
    private java.text.Collator mCollator;

    /**
     * The section array converted from the alphabet string.
     */
    private String[] mAlphabetArray;

    /**
     * Constructs the indexer.
     * @param cursor the cursor containing the data set
     * @param sortedColumnIndex the column number in the cursor that is sorted
     *        alphabetically
     * @param alphabet string containing the alphabet, with space as the first character.
     *        For example, use the string " ABCDEFGHIJKLMNOPQRSTUVWXYZ" for English indexing.
     *        The characters must be uppercase and be sorted in ascii/unicode order. Basically
     *        characters in the alphabet will show up as preview letters.
     */
    public StringArrayAlphabetIndexer(String [] array, CharSequence alphabet) {
    	mArray = array;
        mAlphabet = alphabet;
        mAlphabetLength = alphabet.length();
        mAlphabetArray = new String[mAlphabetLength];
        for (int i = 0; i < mAlphabetLength; i++) {
            mAlphabetArray[i] = Character.toString(mAlphabet.charAt(i));
        }
        mAlphaMap = new SparseIntArray(mAlphabetLength);

        // Get a Collator for the current locale for string comparisons.
        mCollator = java.text.Collator.getInstance();
        mCollator.setStrength(java.text.Collator.PRIMARY);
    }

    /**
     * Returns the section array constructed from the alphabet provided in the constructor.
     * @return the section array
     */
    public Object[] getSections() {
        return mAlphabetArray;
    }

    /**
     * Default implementation compares the first character of word with letter.
     */
    protected int compare(String word, String letter) {
        final String firstLetter;
        if (word.length() == 0) {
            firstLetter = " ";
        } else {
            firstLetter = word.substring(0, 1);
        }

        return mCollator.compare(firstLetter, letter);
    }

    /**
     * Performs a binary search or cache lookup to find the first row that
     * matches a given section's starting letter.
     * @param sectionIndex the section to search for
     * @return the row index of the first occurrence, or the nearest next letter.
     * For instance, if searching for "T" and no "T" is found, then the first
     * row starting with "U" or any higher letter is returned. If there is no
     * data following "T" at all, then the list size is returned.
     */
    public int getPositionForSection(int sectionIndex) {
        final SparseIntArray alphaMap = mAlphaMap;
        final String [] array = mArray;

        if (array == null || mAlphabet == null) {
            return 0;
        }

        // Check bounds
        if (sectionIndex <= 0) {
            return 0;
        }
        if (sectionIndex >= mAlphabetLength) {
            sectionIndex = mAlphabetLength - 1;
        }

        int count = array.length;
        int start = 0;
        int end = count;
        int pos;

        char letter = mAlphabet.charAt(sectionIndex);
        String targetLetter = Character.toString(letter);
        int key = letter;
        // Check map
        if (Integer.MIN_VALUE != (pos = alphaMap.get(key, Integer.MIN_VALUE))) {
            // Is it approximate? Using negative value to indicate that it's
            // an approximation and positive value when it is the accurate
            // position.
            if (pos < 0) {
                pos = -pos;
                end = pos;
            } else {
                // Not approximate, this is the confirmed start of section, return it
                return pos;
            }
        }

        // Do we have the position of the previous section?
        if (sectionIndex > 0) {
            int prevLetter =
                    mAlphabet.charAt(sectionIndex - 1);
            int prevLetterPos = alphaMap.get(prevLetter, Integer.MIN_VALUE);
            if (prevLetterPos != Integer.MIN_VALUE) {
                start = Math.abs(prevLetterPos);
            }
        }

        // Now that we have a possibly optimized start and end, let's binary search

        pos = (end + start) / 2;

        while (pos < end) {
            // Get letter at pos
            String curName = array[pos];
            if (curName == null) {
                if (pos == 0) {
                    break;
                } else {
                    pos--;
                    continue;
                }
            }
            int diff = compare(curName, targetLetter);
            if (diff != 0) {
                // TODO: Commenting out approximation code because it doesn't work for certain
                // lists with custom comparators
                // Enter approximation in hash if a better solution doesn't exist
                // String startingLetter = Character.toString(getFirstLetter(curName));
                // int startingLetterKey = startingLetter.charAt(0);
                // int curPos = alphaMap.get(startingLetterKey, Integer.MIN_VALUE);
                // if (curPos == Integer.MIN_VALUE || Math.abs(curPos) > pos) {
                //     Negative pos indicates that it is an approximation
                //     alphaMap.put(startingLetterKey, -pos);
                // }
                // if (mCollator.compare(startingLetter, targetLetter) < 0) {
                if (diff < 0) {
                    start = pos + 1;
                    if (start >= count) {
                        pos = count;
                        break;
                    }
                } else {
                    end = pos;
                }
            } else {
                // They're the same, but that doesn't mean it's the start
                if (start == pos) {
                    // This is it
                    break;
                } else {
                    // Need to go further lower to find the starting row
                    end = pos;
                }
            }
            pos = (start + end) / 2;
        }
        alphaMap.put(key, pos);
        return pos;
    }

    /**
     * Returns the section index for a given position in the list by querying the item
     * and comparing it with all items in the section array.
     */
    public int getSectionForPosition(int position) {
        String curName = mArray[position];
        // Linear search, as there are only a few items in the section index
        // Could speed this up later if it actually gets used.
        for (int i = 0; i < mAlphabetLength; i++) {
            char letter = mAlphabet.charAt(i);
            String targetLetter = Character.toString(letter);
            if (compare(curName, targetLetter) == 0) {
                return i;
            }
        }
        return 0; // Don't recognize the letter - falls under zero'th section
    }

}
