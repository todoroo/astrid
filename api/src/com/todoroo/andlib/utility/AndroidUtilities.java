package com.todoroo.andlib.utility;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.service.ExceptionService;

/**
 * Android Utility Classes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AndroidUtilities {

    public static final String SEPARATOR_ESCAPE = "!PIPE!"; //$NON-NLS-1$
    public static final String SERIALIZATION_SEPARATOR = "|"; //$NON-NLS-1$

    // --- utility methods

    /** Suppress virtual keyboard until user's first tap */
    public static void suppressVirtualKeyboard(final TextView editor) {
        final int inputType = editor.getInputType();
        editor.setInputType(InputType.TYPE_NULL);
        editor.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                editor.setInputType(inputType);
                editor.setOnTouchListener(null);
                return false;
            }
        });
    }

    /**
     * @return true if we're connected to the internet
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null)
            return false;
        if (info.getState() != State.CONNECTED)
            return false;
        return true;
    }

    /** Fetch the image specified by the given url */
    public static Bitmap fetchImage(URL url) throws IOException {
        InputStream is = null;
        try {
            URLConnection conn = url.openConnection();
            conn.connect();
            is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is, 16384);
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(bis);
                return bitmap;
            } finally {
                bis.close();
            }
        } finally {
            if(is != null)
                is.close();
        }
    }

    /**
     * Start the given intent, handling security exceptions if they arise
     *
     * @param context
     * @param intent
     * @param request request code. if negative, no request.
     */
    public static void startExternalIntent(Context context, Intent intent, int request) {
        try {
            if(request > -1 && context instanceof Activity)
                ((Activity)context).startActivityForResult(intent, request);
            else
                context.startActivity(intent);
        } catch (Exception e) {
            getExceptionService().displayAndReportError(context,
                    "start-external-intent-" + intent.toString(), //$NON-NLS-1$
                    e);
        }
    }

    /**
     * Start the given intent, handling security exceptions if they arise
     *
     * @param activity
     * @param intent
     * @param requestCode
     */
    public static void startExternalIntentForResult(
            Activity activity, Intent intent, int requestCode) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (SecurityException e) {
            getExceptionService().displayAndReportError(activity,
                    "start-external-intent-" + intent.toString(), //$NON-NLS-1$
                    e);
        }
    }

    /**
     * Put an arbitrary object into a {@link ContentValues}
     * @param target
     * @param key
     * @param value
     */
    public static void putInto(ContentValues target, String key, Object value) {
        if(value instanceof String)
            target.put(key, (String) value);
        else if(value instanceof Long)
            target.put(key, (Long) value);
        else if(value instanceof Integer)
            target.put(key, (Integer) value);
        else if(value instanceof Double)
            target.put(key, (Double) value);
        else
            throw new UnsupportedOperationException("Could not handle type " + //$NON-NLS-1$
                    value.getClass());
    }

    /**
     * Rips apart a content value into two string arrays, keys and value
     */
    public static String[][] contentValuesToStringArrays(ContentValues source) {
        String[][] result = new String[2][source.size()];
        int i = 0;
        for(Entry<String, Object> entry : source.valueSet()) {
            result[0][i] = entry.getKey();
            result[1][i++] = entry.getValue().toString();
        }
        return result;
    }

    /**
     * Return index of value in array
     * @param array array to search
     * @param value value to look for
     * @return
     */
    public static <TYPE> int indexOf(TYPE[] array, TYPE value) {
        for(int i = 0; i < array.length; i++)
            if(array[i].equals(value))
                return i;
        return -1;
    }

    /**
     * Serializes a content value into a string
     */
    public static String contentValuesToSerializedString(ContentValues source) {
        StringBuilder result = new StringBuilder();
        for(Entry<String, Object> entry : source.valueSet()) {
            result.append(entry.getKey().replace(SERIALIZATION_SEPARATOR, SEPARATOR_ESCAPE)).append(
                    SERIALIZATION_SEPARATOR);
            Object value = entry.getValue();
            if(value instanceof Integer)
                result.append('i').append(value);
            else if(value instanceof Double)
                result.append('d').append(value);
            else if(value instanceof Long)
                result.append('l').append(value);
            else if(value instanceof String)
                result.append('s').append(value.toString());
            else
                throw new UnsupportedOperationException(value.getClass().toString());
            result.append(SERIALIZATION_SEPARATOR);
        }
        return result.toString();
    }

    /**
     * Turn ContentValues into a string
     * @param string
     * @return
     */
    public static ContentValues contentValuesFromSerializedString(String string) {
        if(string == null)
            return new ContentValues();

        String[] pairs = string.split("\\" + SERIALIZATION_SEPARATOR); //$NON-NLS-1$
        ContentValues result = new ContentValues();
        for(int i = 0; i < pairs.length; i += 2) {
            String key = pairs[i].replaceAll(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR);
            String value = pairs[i+1].substring(1);
            try {
                switch(pairs[i+1].charAt(0)) {
                case 'i':
                    result.put(key, Integer.parseInt(value));
                    break;
                case 'd':
                    result.put(key, Double.parseDouble(value));
                    break;
                case 'l':
                    result.put(key, Long.parseLong(value));
                    break;
                case 's':
                    result.put(key, value.replace(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR));
                    break;
                }
            } catch (NumberFormatException e) {
                // failed parse to number, try to put a string
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Turn ContentValues into a string
     * @param string
     * @return
     */
    @SuppressWarnings("nls")
    public static ContentValues contentValuesFromString(String string) {
        if(string == null)
            return null;

        String[] pairs = string.split("=");
        ContentValues result = new ContentValues();
        String key = null;
        for(int i = 0; i < pairs.length; i++) {
            String newKey = null;
            int lastSpace = pairs[i].lastIndexOf(' ');
            if(lastSpace != -1) {
                newKey = pairs[i].substring(lastSpace + 1);
                pairs[i] = pairs[i].substring(0, lastSpace);
            } else {
                newKey =  pairs[i];
            }
            if(key != null)
                result.put(key.trim(), pairs[i].trim());
            key = newKey;
        }
        return result;
    }

    /**
     * Returns true if a and b or null or a.equals(b)
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(Object a, Object b) {
        if(a == null && b == null)
            return true;
        if(a == null)
            return false;
        return a.equals(b);
    }

    /**
     * Copy a file from one place to another
     *
     * @param in
     * @param out
     * @throws Exception
     */
    public static void copyFile(File in, File out) throws Exception {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        try {
            copyStream(fis, fos);
        } catch (Exception e) {
            throw e;
        } finally {
            fis.close();
            fos.close();
        }
    }

    /**
     * Copy stream from source to destination
     * @param source
     * @param dest
     * @throws IOException
     */
    public static void copyStream(InputStream source, OutputStream dest) throws IOException {
        int bytes;
        byte[] buffer;
        int BUFFER_SIZE = 1024;
        buffer = new byte[BUFFER_SIZE];
        while ((bytes = source.read(buffer)) != -1) {
            if (bytes == 0) {
                bytes = source.read();
                if (bytes < 0)
                    break;
                dest.write(bytes);
                dest.flush();
                continue;
            }

            dest.write(buffer, 0, bytes);
            dest.flush();
        }
    }

    /**
     * Find a child view of a certain type
     * @param view
     * @param type
     * @return first view (by DFS) if found, or null if none
     */
    public static <TYPE> TYPE findViewByType(View view, Class<TYPE> type) {
        if(view == null)
            return null;
        if(type.isInstance(view))
            return (TYPE) view;
        if(view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for(int i = 0; i < group.getChildCount(); i++) {
                TYPE v = findViewByType(group.getChildAt(i), type);
                if(v != null)
                    return v;
            }
        }
        return null;
    }

    /**
     * @return Android SDK version as an integer. Works on all versions
     */
    public static int getSdkVersion() {
        return Integer.parseInt(android.os.Build.VERSION.SDK);
    }

    /**
     * Copy databases to a given folder. Useful for debugging
     * @param folder
     */
    public static void copyDatabases(Context context, String folder) {
        File folderFile = new File(folder);
        if(!folderFile.exists())
            folderFile.mkdir();
        for(String db : context.databaseList()) {
            File dbFile = context.getDatabasePath(db);
            try {
                copyFile(dbFile, new File(folderFile.getAbsolutePath() +
                        File.separator + db));
            } catch (Exception e) {
                Log.e("ERROR", "ERROR COPYING DB " + db, e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * Sort files by date so the newest file is on top
     * @param files
     */
    public static void sortFilesByDateDesc(File[] files) {
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return Long.valueOf(o2.lastModified()).compareTo(Long.valueOf(o1.lastModified()));
            }
        });
    }

    /**
     * Search for the given value in the map, returning key if found
     * @param map
     * @param value
     * @return null if not found, otherwise key
     */
    public static <KEY, VALUE> KEY findKeyInMap(Map<KEY, VALUE> map, VALUE value){
        for (Entry<KEY, VALUE> entry: map.entrySet()) {
            if(entry.getValue().equals(value))
                return entry.getKey();
        }
        return null;
    }

    /**
     * Sleep, ignoring interruption. Before using this method, think carefully
     * about why you are ignoring interruptions.
     *
     * @param l
     */
    public static void sleepDeep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    /**
     * Call a method via reflection if API level is at least minSdk
     * @param minSdk minimum sdk number (i.e. 8)
     * @param receiver object to call method on
     * @param methodName method name to call
     * @param params method parameter types
     * @param args arguments
     * @return method return value, or null if nothing was called or exception
     */
    @SuppressWarnings("nls")
    public static Object callApiMethod(int minSdk, Object receiver,
            String methodName, Class<?>[] params, Object... args) {
        if(getSdkVersion() < minSdk)
            return null;

        Method method;
        try {
            method = receiver.getClass().getMethod(methodName, params);
            return method.invoke(receiver, args);
        } catch (SecurityException e) {
            getExceptionService().reportError("call-method", e);
        } catch (NoSuchMethodException e) {
            getExceptionService().reportError("call-method", e);
        } catch (IllegalArgumentException e) {
            getExceptionService().reportError("call-method", e);
        } catch (IllegalAccessException e) {
            getExceptionService().reportError("call-method", e);
        } catch (InvocationTargetException e) {
            getExceptionService().reportError("call-method", e);
        }

        return null;
    }

    /**
     * From Android MyTracks project (http://mytracks.googlecode.com/).
     * Licensed under the Apache Public License v2
     * @param activity
     * @param id
     * @return
     */
    public static CharSequence readFile(Context activity, int id) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    activity.getResources().openRawResource(id)));
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            return buffer;
        } catch (IOException e) {
            return ""; //$NON-NLS-1$
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Performs an md5 hash on the input string
     * @param input
     * @return
     */
    @SuppressWarnings("nls")
    public static String md5(String input) {
        try {
            byte[] bytesOfMessage = input.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytesOfMessage);
            BigInteger bigInt = new BigInteger(1,digest);
            String hashtext = bigInt.toString(16);
            while(hashtext.length() < 32 ){
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Create an intent to a remote activity
     * @param appPackage
     * @param activityClass
     * @return
     */
    public static Intent remoteIntent(String appPackage, String activityClass) {
        Intent intent = new Intent();
        intent.setClassName(appPackage, activityClass);
        return intent;
    }

    /**
     * Gets application signature
     * @return application signature, or null if an error was encountered
     */
    public static String getSignature(Context context, String packageName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);
            return packageInfo.signatures[0].toCharsString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Join items to a list
     * @param <TYPE>
     * @param list
     * @param newList
     * @param newItems
     * @return
     */
    public static Property<?>[] addToArray(Property<?>[] list, Property<?>... newItems) {
        Property<?>[] newList = new Property<?>[list.length + newItems.length];
        for(int i = 0; i < list.length; i++)
            newList[i] = list[i];
        for(int i = 0; i < newItems.length; i++)
            newList[list.length + i] = newItems[i];
        return newList;
    }

    // --- internal

    private static ExceptionService exceptionService = null;

    private static ExceptionService getExceptionService() {
        if(exceptionService == null)
            synchronized(AndroidUtilities.class) {
                if(exceptionService == null)
                    exceptionService = new ExceptionService();
            }
        return exceptionService;
    }

    /**
     * Concatenate additional stuff to the end of the array
     * @param params
     * @param additional
     * @return
     */
    public static <TYPE> TYPE[] concat(TYPE[] dest, TYPE[] source, TYPE... additional) {
        int i = 0;
        for(; i < Math.min(dest.length, source.length); i++)
            dest[i] = source[i];
        int base = i;
        for(; i < dest.length; i++)
            dest[i] = additional[i - base];
        return dest;
    }

}
