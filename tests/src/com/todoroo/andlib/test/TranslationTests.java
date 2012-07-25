/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.test;



import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.res.Resources;

/**
 * Tests translations for consistency with the default values. You must
 * extend this class and create it with your own values for strings
 * and arrays.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class TranslationTests extends TodorooTestCase {

    // --- abstract methods

    /**
     * @return R.string.class
     */
    public abstract Class<?> getStringResources();

    /**
     * @return R.array.class
     */
    public abstract Class<?> getArrayResources();

    /**
     * @return array of fields that are parsed by SimpleDateFormat
     */
    public abstract int[] getDateFormatStrings();

    // --- tests

    private static final class FormatStringData {
        private static final char[] scratch = new char[10];

        /** format characters */
        public final char[] characters;

        /** the original string */
        public final String string;

        public FormatStringData(String string) {
            this.string = string;

            int pos = -1;
            int count = 0;
            while(true) {
                pos = string.indexOf('%', ++pos);
                if(pos++ == -1)
                    break;
                if(pos >= string.length())
                    scratch[count++] = '\0';
                else
                    scratch[count++] = string.charAt(pos);
            }
            characters = new char[count];
            for(int i = 0; i < count; i++) {
                characters[i] = scratch[i];
            }
        }

        /** test that the characters match */
        public boolean matches(FormatStringData other) {
            if(characters.length != other.characters.length)
                return false;
            outer: for(int i = 0; i < characters.length; i++) {
                if(Character.isDigit(characters[i])) {
                    for(int j = 0; j < other.characters.length; j++)
                        if(characters[i] == other.characters[j])
                            break outer;
                    return false;
                } else if(characters[i] != other.characters[i])
                    return false;

            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder value = new StringBuilder("[");
            for(int i = 0; i < characters.length; i++) {
                value.append(characters[i]);
                if(i < characters.length - 1)
                    value.append(',');
            }
            value.append("]: '").append(string).append('\'');
            return value.toString();
        }
    }

    /**
     * Internal test of format string parser
     */
    public void testFormatStringParser() {
        String s = "abc";
        FormatStringData data = new FormatStringData(s);
        assertEquals(s, data.string);
        assertEquals(0, data.characters.length);

        s = "abc %s def";
        data = new FormatStringData(s);
        assertEquals(1, data.characters.length);
        assertEquals('s', data.characters[0]);

        s = "abc %%s def %d";
        data = new FormatStringData(s);
        assertEquals(2, data.characters.length);
        assertEquals('%', data.characters[0]);
        assertEquals('d', data.characters[1]);
        assertTrue(data.toString(), data.toString().contains("[%"));
        assertTrue(data.toString(), data.toString().contains("d]"));
        assertTrue(data.toString(), data.toString().contains(s));
        assertTrue(data.matches(new FormatStringData("espanol %% und %d si")));
        assertFalse(data.matches(new FormatStringData("ingles %d ja %% pon")));

        s = "% abc %";
        data = new FormatStringData(s);
        assertEquals(2, data.characters.length);
        assertEquals(' ', data.characters[0]);
        assertEquals('\0', data.characters[1]);
    }

    /**
     * Test that the format specifiers in translations match exactly the
     * translations in the default text
     */
    public void testFormatStringsMatch() throws Exception {
        final Resources r = getContext().getResources();
        final int[] strings = getResourceIds(getStringResources());
        final FormatStringData[] formatStrings = new FormatStringData[strings.length];

        final StringBuilder failures = new StringBuilder();

        for(int i = 0; i < strings.length; i++) {
            try {
                String string = r.getString(strings[i]);
                formatStrings[i] = new FormatStringData(string);
            } catch (Exception e) {
                String name = r.getResourceName(strings[i]);
                failures.append(String.format("error opening %s: %s\n",
                        name, e.getMessage()));
            }
        }


        forEachLocale(new Runnable() {
            public void run() {
                Locale locale = r.getConfiguration().locale;
                for(int i = 0; i < strings.length; i++) {
                    try {
                        String string = r.getString(strings[i]);
                        FormatStringData newFS = new FormatStringData(string);
                        if(!newFS.matches(formatStrings[i])) {
                            String name = r.getResourceName(strings[i]);
                            failures.append(String.format("%s (%s): %s != %s\n",
                                    name, locale.toString(), newFS, formatStrings[i]));
                        }
                    } catch (Exception e) {
                        String name = r.getResourceName(strings[i]);
                        failures.append(String.format("%s: error opening %s: %s\n",
                                locale.toString(), name, e.getMessage()));
                    }
                }
            }
        });

        assertTrue(failures.toString(), errorCount(failures) == 0);
    }

    /**
     * Test that date formatters parse correctly
     */
    public void testDateFormats() throws Exception {
        final Resources r = getContext().getResources();

        final StringBuilder failures = new StringBuilder();
        final int[] dateStrings = getDateFormatStrings();
        final Date date = new Date();

        forEachLocale(new Runnable() {
            public void run() {
                Locale locale = r.getConfiguration().locale;
                for(int i = 0; i < dateStrings.length; i++) {
                    try {
                        String string = r.getString(dateStrings[i]);
                        try {
                            new SimpleDateFormat(string).format(date);
                        } catch (Exception e) {
                            String name = r.getResourceName(dateStrings[i]);
                            failures.append(String.format("%s: invalid format string '%s': %s\n",
                                    locale.toString(), name, e.getMessage()));
                        }
                    } catch (Exception e) {
                        String name = r.getResourceName(dateStrings[i]);
                        failures.append(String.format("%s: error opening %s: %s\n",
                                locale.toString(), name, e.getMessage()));
                    }
                }
            }
        });

        assertEquals(failures.toString(), 0, errorCount(failures));

    }

    /**
     * Test that there are the same number of array entries in each locale
     */
    public void testArraySizesMatch() throws Exception {
        final Resources r = getContext().getResources();
        final int[] arrays = getResourceIds(getArrayResources());
        final int[] sizes = new int[arrays.length];
        final StringBuilder failures = new StringBuilder();

        for(int i = 0; i < arrays.length; i++) {
            try {
                sizes[i] = r.getStringArray(arrays[i]).length;
            } catch (Resources.NotFoundException e) {
                String name = r.getResourceName(arrays[i]);
                failures.append(String.format("error opening %s: %s\n",
                        name, e.getMessage()));
                sizes[i] = -1;
            }
        }

        forEachLocale(new Runnable() {
            public void run() {
                for(int i = 0; i < arrays.length; i++) {
                    if(sizes[i] == -1)
                        continue;
                    int size;
                    try {
                        size = r.getStringArray(arrays[i]).length;
                    } catch (Resources.NotFoundException e) {
                        String name = r.getResourceName(arrays[i]);
                        Locale locale = r.getConfiguration().locale;
                        failures.append(String.format("%s: error opening %s: %s\n",
                                locale, name, e.getMessage()));
                        continue;
                    }

                    if(size != sizes[i]) {
                        String name = r.getResourceName(arrays[i]);
                        Locale locale = r.getConfiguration().locale;
                        failures.append(String.format("%s (%s): size %d != %d\n",
                                name, locale.toString(), size, sizes[i]));
                    }
                }
            }
        });
        assertEquals(failures.toString(), 0, errorCount(failures));
    }

    // --- helper methods

    /**
     * Count newlines
     */
    private int errorCount(StringBuilder failures) {
        int count = 0;
        int pos = -1;
        while(true) {
            pos = failures.indexOf("\n", pos + 1);
            if(pos == -1)
                return count;
            count++;
        }
    }

    /**
     * @return an array of all string resource id's
     */
    public int[] getResourceIds(Class<?> resources) throws Exception {
        Field[] fields = resources.getDeclaredFields();
        List<Integer> ids = new ArrayList<Integer>(fields.length);
        for(int i = 0; i < fields.length; i++) {
            try {
                ids.add(fields[i].getInt(null));
            } catch (Exception e) {
                // not a field we care about
            }
        }
        int[] idsAsIntArray = new int[ids.size()];
        for(int i = 0; i < ids.size(); i++)
            idsAsIntArray[i] = ids.get(i);
        return idsAsIntArray;
    }

}
