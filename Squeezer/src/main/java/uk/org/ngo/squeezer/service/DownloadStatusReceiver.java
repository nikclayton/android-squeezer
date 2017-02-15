/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.service;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;


/**
 * Handle events from the download manager
 * <p/>
 * This class is registered in the manifest.
 */
public class DownloadStatusReceiver extends BroadcastReceiver {
    private static final String TAG = DownloadStatusReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
                handleUserRequest(context);
            }
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                handleDownloadComplete(context, intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void handleUserRequest(Context context) {
        Log.i(TAG, "Download notification clicked");
        Intent intent = new Intent(context, CancelDownloadsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void handleDownloadComplete(Context context, long id) {
        final DownloadDatabase downloadDatabase = new DownloadDatabase(context);
        final DownloadManager downloadManager =(DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
        final DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
        final Cursor cursor = downloadManager.query(query);
        try {
            while (cursor.moveToNext()) {
                int downloadId = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                String title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                String url = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));
                String local_url = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                DownloadDatabase.DownloadEntry downloadEntry = downloadDatabase.popDownloadEntry(downloadId);

                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), downloadEntry.tempName);
                        File localFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), downloadEntry.fileName);
                        File localFolder = localFile.getParentFile();
                        if (!localFolder.exists())
                            localFolder.mkdirs();
                        if (!tempFile.renameTo(localFile)) {
                            Log.w(TAG, "Could not rename [" + tempFile + "] to [" + localFile + "]");
                        }
                        break;
                    default:
                        Log.w(TAG, "Unsuccessful download (status:" + status + ", reason:" + reason + ", title:'" + title + "', url:'" + url + "', local url:'" + local_url + "')");
                        break;
                }
            }
        } finally {
            cursor.close();
        }
    }
}
