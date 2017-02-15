package uk.org.ngo.squeezer.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import uk.org.ngo.squeezer.Preferences;

public class SqueezePlayer extends Handler {
    private static final int MSG_STOP = 0;
    private static final int MSG_TIMEOUT = 1;
    private static final long TIMEOUT_DELAY = 10 * 60 * 1000; // 10 minutes in milliseconds

    private static final String SQUEEZEPLAYER_PACKAGE = "de.bluegaspode.squeezeplayer";
    private static final String SQUEEZEPLAYER_SERVICE = "de.bluegaspode.squeezeplayer.playback.service.PlaybackService";
    private static final String HAS_SERVER_SETTINGS_EXTRA = "intentHasServerSettings";
    private static final String FORCE_SERVER_SETTINGS_EXTRA = "forceSettingsFromIntent";
    private static final String SERVER_URL_EXTRA = "serverURL";
    private static final String SERVER_NAME_EXTRA = "serverName";
    private static final String USER_NAME_EXTRA = "username";
    private static final String PASSWORD_EXTRA = "password";

    private static final String TAG = "SqueezePlayer";

    private final String serverUrl;
    private final String serverName;
    private final String userName;
    private final String password;
    private final Context context;

    public SqueezePlayer(Context context) {
        this.context = context;

        Preferences preferences = new Preferences(context);
        serverUrl = preferences.getServerAddress();
        serverName = preferences.getServerName();
        userName = preferences.getUserName();
        password = preferences.getPassword();

        Log.d(TAG, "startControllingSqueezePlayer");
        startControllingSqueezePlayer();
    }

    public static boolean hasSqueezePlayer(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(SQUEEZEPLAYER_PACKAGE);
        return (intent != null);
    }

    private void startControllingSqueezePlayer() {
        context.startService(getSqueezePlayerIntent());
        sendMessageDelayed(obtainMessage(MSG_TIMEOUT), TIMEOUT_DELAY);
    }

    private Intent getSqueezePlayerIntent() {
        final ComponentName component = new ComponentName(SQUEEZEPLAYER_PACKAGE, SQUEEZEPLAYER_SERVICE);
        Intent intent = new Intent().setComponent(component);
        if (serverUrl != null) {
            intent.putExtra(FORCE_SERVER_SETTINGS_EXTRA, true);
            intent.putExtra(HAS_SERVER_SETTINGS_EXTRA, true);
            intent.putExtra(SERVER_URL_EXTRA, serverUrl);
            intent.putExtra(SERVER_NAME_EXTRA, serverName);
            if (userName != null)
                intent.putExtra(USER_NAME_EXTRA, userName);
            if (password != null)
                intent.putExtra(PASSWORD_EXTRA, password);
        }
        return intent;
    }

    public void stopControllingSqueezePlayer() {
        sendEmptyMessage(MSG_STOP);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_STOP :
                removeMessages(MSG_TIMEOUT);
                break;
            case MSG_TIMEOUT:
                startControllingSqueezePlayer();
                break;
        }
    }
}
