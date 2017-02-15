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
package uk.org.ngo.squeezer;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.service.ISqueezeService;


public class VolumePanel extends Handler implements SeekBar.OnSeekBarChangeListener {

    private static final int TIMEOUT_DELAY = 3000;

    private static final int MSG_VOLUME_CHANGED = 0;

    private static final int MSG_FREE_RESOURCES = 1;

    private static final int MSG_TIMEOUT = 2;

    protected final BaseActivity mActivity;

    /**
     * Dialog displaying the volume panel.
     */
    private final Dialog mDialog;

    /**
     * View displaying volume sliders.
     */
    private final View mView;

    private final TextView mMessage;

    private final TextView mAdditionalMessage;

    private final ImageView mLargeStreamIcon;

    private final SeekBar mSeekbar;

    @SuppressLint({"InflateParams"}) // OK, as view is passed to Dialog.setView()
    public VolumePanel(BaseActivity activity) {
        mActivity = activity;

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.volume_adjust, null);
        mView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                resetTimeout();
                return false;
            }
        });

        mMessage = (TextView) mView.findViewById(R.id.message);
        mAdditionalMessage = (TextView) mView.findViewById(R.id.additional_message);
        mSeekbar = (SeekBar) mView.findViewById(R.id.level);
        mLargeStreamIcon = (ImageView) mView.findViewById(R.id.ringer_stream_icon);

        mSeekbar.setOnSeekBarChangeListener(this);

        mDialog = new Dialog(mActivity, R.style.VolumePanel) { //android.R.style.Theme_Panel) {
            public boolean onTouchEvent(MotionEvent event) {
                if (isShowing() && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    forceTimeout();
                    return true;
                }
                return false;
            }
        };
        mDialog.setTitle("Volume Control");
        mDialog.setContentView(mView);

        // Set window properties to match other toasts/dialogs.
        Window window = mDialog.getWindow();
        window.setGravity(Gravity.TOP);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = null;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
    }

    public void dismiss() {
        removeMessages(MSG_TIMEOUT);
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private void resetTimeout() {
        removeMessages(MSG_TIMEOUT);
        sendMessageDelayed(obtainMessage(MSG_TIMEOUT), TIMEOUT_DELAY);
    }

    private void forceTimeout() {
        removeMessages(MSG_TIMEOUT);
        sendMessage(obtainMessage(MSG_TIMEOUT));
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            ISqueezeService service = mActivity.getService();
            if (service != null) {
                service.adjustVolumeTo(progress);
            }
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void postVolumeChanged(int newVolume, String additionalMessage) {
        if (hasMessages(MSG_VOLUME_CHANGED)) {
            return;
        }
        removeMessages(MSG_FREE_RESOURCES);
        obtainMessage(MSG_VOLUME_CHANGED, newVolume, 0, additionalMessage).sendToTarget();
    }

    protected void onVolumeChanged(int newVolume, String additionalMessage) {
        onShowVolumeChanged(newVolume, additionalMessage);
    }

    protected void onShowVolumeChanged(int newVolume, String additionalMessage) {
        mSeekbar.setMax(100);
        mSeekbar.setProgress(newVolume);

        mMessage.setText(
                mActivity.getString(R.string.volume, mActivity.getString(R.string.app_name)));
        mAdditionalMessage.setText(additionalMessage);

        mLargeStreamIcon.setImageResource(newVolume == 0
                ? R.drawable.ic_volume_off
                : R.drawable.ic_volume);

        if (!mDialog.isShowing() && !mActivity.isFinishing()) {
            mDialog.setContentView(mView);
            mDialog.show();
        }

        resetTimeout();
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

            case MSG_TIMEOUT: {
                dismiss();
                break;
            }

            case MSG_FREE_RESOURCES: {
                onFreeResources();
                break;
            }
        }
    }
}

