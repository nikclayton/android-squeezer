/*
 * Copyright 2012 Google Inc.
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.annotation.Nullable;

import uk.org.ngo.squeezer.R;

/**
 * A subclass of {@link ImageWorker} that fetches images from a URL.
 */
public class ImageFetcher extends ImageWorker {

    private static final String TAG = "ImageFetcher";

    private static final int IO_BUFFER_SIZE = 8 * 1024;

    private volatile static ImageFetcher sImageFetcher;

    private ImageFetcher(Context context) {
        super(context);
    }

    /**
     * @return an imagefetcher globally useful for the application, with a cache that
     *     is maintained across activities.
     *
     * @param context Anything that provides a context.
     */
    @NonNull
    public static ImageFetcher getInstance(Context context) {
        ImageFetcher result = sImageFetcher;
        if (result == null) {
            synchronized (ImageFetcher.class) {
                result = sImageFetcher;
                if (result == null) {
                    sImageFetcher = new ImageFetcher(context);
                    sImageFetcher.setLoadingImage(R.drawable.icon_pending_artwork);
                    ImageCache.ImageCacheParams imageCacheParams = new ImageCache.ImageCacheParams(context, "artwork");
                    imageCacheParams.setMemCacheSizePercent(context, 0.12f);
                    sImageFetcher.addImageCache(imageCacheParams);
                }
            }
        }
        return sImageFetcher;
    }

    /**
     * Call this in low memory situations. Clears the memory cache.
     */
    public static void onLowMemory() {
        if (sImageFetcher == null) {
            return;
        }

        sImageFetcher.clearMemoryCache();
    }

    /**
     * The main process method, which will be called by the ImageWorker in the AsyncTask background
     * thread.
     *
     * @param params The parameters for this request.
     *
     * @return The downloaded bitmap, null if downloading failed.
     */
    @Nullable
    protected Bitmap processBitmap(BitmapWorkerTaskParams params) {
        String data = params.data.toString();
        Log.d(TAG, "processBitmap: " + data);

        disableConnectionReuseIfNecessary();

        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        Bitmap bitmap = null;

        try {
            final URL url = new URL(data);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadUrlToStream - " + data + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
            }
        }

        return bitmap;
    }

    /**
     * Workaround for bug pre-Froyo, see here for more info: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
    public static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }
}
