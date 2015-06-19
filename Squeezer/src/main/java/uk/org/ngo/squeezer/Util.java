/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class Util {
    private static final String TAG = Util.class.getSimpleName();

    private Util() {
    }


    /**
     * Update target, if it's different from newValue.
     *
     * @return true if target is updated. Otherwise return false.
     */
    public static <T> boolean atomicReferenceUpdated(AtomicReference<T> target, T newValue) {
        T currentValue = target.get();
        if (currentValue == null && newValue == null) {
            return false;
        }
        if (currentValue == null || !currentValue.equals(newValue)) {
            target.set(newValue);
            return true;
        }
        return false;
    }

    public static int parseDecimalInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        int decimalPoint = value.indexOf('.');
        if (decimalPoint != -1) {
            value = value.substring(0, decimalPoint);
        }
        if (value.length() == 0) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseDecimalIntOrZero(String value) {
        return parseDecimalInt(value, 0);
    }

    private static final StringBuilder sFormatBuilder = new StringBuilder();

    private static final Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());

    private static final Object[] sTimeArgs = new Object[5];

    /**
     * Formats an elapsed time in the form "M:SS" or "H:MM:SS" for display.
     * <p/>
     * Like {@link android.text.format.DateUtils#formatElapsedTime(long)} but without the leading
     * zeroes if the number of minutes is < 10.
     *
     * @param elapsedSeconds the elapsed time, in seconds.
     */
    public synchronized static String formatElapsedTime(long elapsedSeconds) {
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = elapsedSeconds / 3600;
        timeArgs[1] = elapsedSeconds / 60;
        timeArgs[2] = (elapsedSeconds / 60) % 60;
        timeArgs[3] = elapsedSeconds;
        timeArgs[4] = elapsedSeconds % 60;
        return sFormatter.format("%2$d:%5$02d", timeArgs).toString();
    }

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static String decode(String string) {
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static String parseHost(String hostPort) {
        if (hostPort == null) {
            return "";
        }
        int colonPos = hostPort.indexOf(":");
        if (colonPos == -1) {
            return hostPort;
        }
        return hostPort.substring(0, colonPos);
    }

    public static int parsePort(String hostPort) {
        if (hostPort == null) {
            return Squeezer.getContext().getResources().getInteger(R.integer.DefaultPort);
        }
        int colonPos = hostPort.indexOf(":");
        if (colonPos == -1) {
            return Squeezer.getContext().getResources().getInteger(R.integer.DefaultPort);
        }
        try {
            return Integer.parseInt(hostPort.substring(colonPos + 1));
        } catch (NumberFormatException unused) {
            Log.d(TAG, "Can't parse port out of " + hostPort);
            return Squeezer.getContext().getResources().getInteger(R.integer.DefaultPort);
        }
    }

    /**
     *
     * @param context
     * @param convertView
     * @param parent
     * @param label
     * @return a view suitable for use as a spinner view.
     */
    public static View getSpinnerItemView(Context context, View convertView, ViewGroup parent,
                                          String label) {
        return getSpinnerView(context, convertView, parent, label,
                android.R.layout.simple_spinner_item);
    }

    /**
     *
     * @param context
     * @param convertView
     * @param parent
     * @param label
     * @return a view suitable for use in a spinner's dropdown menu.
     */
    public static View getSpinnerDropDownView(Context context, View convertView, ViewGroup parent,
                                      String label) {
        return getSpinnerView(context, convertView, parent, label,
                android.R.layout.simple_spinner_dropdown_item);
    }

    public static View getActionBarSpinnerItemView(Context context, View convertView,
                                                   ViewGroup parent, String label) {
        return getSpinnerView(context, convertView, parent, label,
                android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);
    }

    private static View getSpinnerView(Context context, View convertView, ViewGroup parent,
                                       String label, int layout) {
        TextView view;
        view = (TextView) (convertView != null
                && TextView.class.isAssignableFrom(convertView.getClass())
                ? convertView
                : ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                        layout, parent, false));
        view.setText(label);
        return view;
    }

    /**
     * Count how many of the supplied booleans are true.
     *
     * @param items Booleans to count
     *
     * @return Number of arguments which are true
     */
    public static int countBooleans(boolean... items) {
        int count = 0;
        for (boolean item : items) {
            if (item) {
                count++;
            }
        }
        return count;
    }

    /** Helper to set alpha value for a view, since View.setAlpha is API level 11 */
    public static View setAlpha(View view, float alpha) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(alpha, alpha);
        alphaAnimation.setDuration(0);
        alphaAnimation.setFillAfter(true);
        view.startAnimation(alphaAnimation);

        return view;
    }
}
