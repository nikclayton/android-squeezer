/*
 * Copyright 2013 Google Inc.
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

package uk.org.ngo.squeezer.util;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import java.util.Hashtable;
import java.util.Map;

/**
 * Provides a fragment that will be retained across the lifecycle of the activity that hosts it.
 * <p/>
 * Get an instance of this class by calling {@link #getInstance(String, FragmentManager)}, and place
 * objects that should be persisted across the activity lifecycle using {@link #put(String,
 * Object)}. Retrieve persisted objects with {@link #get(String)}.
 */
public class RetainFragment extends Fragment {

    private static final String TAG = RetainFragment.class.getName();

    private final Map<String, Object> mHash = new Hashtable<String, Object>();

    /**
     * Empty constructor as per the Fragment documentation
     */
    public RetainFragment() {
    }

    public static RetainFragment getInstance(String tag, FragmentManager fm) {
        Log.d(TAG, "getInstance() for " + tag);
        RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(tag);
        if (fragment == null) {
            Log.d(TAG, "  Creating new instance");
            fragment = new RetainFragment();
            fm.beginTransaction().add(fragment, tag).commitAllowingStateLoss();
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure this Fragment is retained over a configuration change
        setRetainInstance(true);
    }

    public Object put(String key, Object value) {
        mHash.put(key, value);
        return value;
    }

    public Object get(String key) {
        return mHash.get(key);
    }
}
