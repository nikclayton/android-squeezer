/*
 * Copyright (c) 2011 Google Inc.  All Rights Reserved.
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

/**
 * Implement a custom toast view that's modelled on the one in
 * android.view.VolumePanel (but which is not public).
 *
 */
package com.danga.squeezer;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class VolumePanel extends Handler {
	private static final int MSG_VOLUME_CHANGED = 0;
    private static final int MSG_FREE_RESOURCES = 1;

	protected Context mContext;

    private final Toast mToast;
    private final View mView;
    private final TextView mMessage;
    private final TextView mAdditionalMessage;
    private final ImageView mLargeStreamIcon;
    private final ProgressBar mLevel;

    public VolumePanel(Context context) {
    	mContext = context;
        mToast = new Toast(context);

        LayoutInflater inflater = (LayoutInflater) context
        		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.volume_adjust, null);
        mMessage = (TextView) mView.findViewById(R.id.message);
    	mAdditionalMessage = (TextView) mView.findViewById(R.id.additional_message);
    	mLevel = (ProgressBar) mView.findViewById(R.id.level);
    	mLargeStreamIcon = (ImageView) mView.findViewById(R.id.ringer_stream_icon);
    }

    public void postVolumeChanged(int newVolume, String additionalMessage) {
        if (hasMessages(MSG_VOLUME_CHANGED)) return;
        removeMessages(MSG_FREE_RESOURCES);
        obtainMessage(MSG_VOLUME_CHANGED, newVolume, 0, additionalMessage).sendToTarget();
    }

    protected void onVolumeChanged(int newVolume, String additionalMessage) {
    	onShowVolumeChanged(newVolume, additionalMessage);
    }

    protected void onShowVolumeChanged(int newVolume, String additionalMessage) {
    	mLevel.setMax(100);
    	mLevel.setProgress(newVolume);

    	mMessage.setText(mContext.getString(R.string.volume, mContext.getString(R.string.app_name)));
    	mAdditionalMessage.setText(additionalMessage);

		mLargeStreamIcon.setImageResource(newVolume == 0
				? R.drawable.ic_volume_off
				: R.drawable.ic_volume);

    	mToast.setView(mView);
    	mToast.setDuration(Toast.LENGTH_SHORT);
    	mToast.setGravity(Gravity.TOP, 0, 0);
    	mToast.show();
    }

    protected void onFreeResources() {
        // We'll keep the views, just ditch the cached drawable and hence
        // bitmaps
        mLargeStreamIcon.setImageDrawable(null);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {

            case MSG_VOLUME_CHANGED: {
                onVolumeChanged(msg.arg1, (String) msg.obj);
                break;
            }

            case MSG_FREE_RESOURCES: {
                onFreeResources();
                break;
            }
        }
    }
}