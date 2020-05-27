package uk.org.ngo.squeezer.homescreenwidgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;

import androidx.annotation.LayoutRes;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.PlayerBaseView;
import uk.org.ngo.squeezer.itemlist.PlayerListBaseActivity;
import uk.org.ngo.squeezer.model.Player;

/**
 * The configuration screen for the {@link SqueezerRemoteControl SqueezerRemoteControl} AppWidget.
 */
public class SqueezerRemoteControlConfigureActivity extends PlayerListBaseActivity {

    public static final String UNKNOWN_PLAYER = "UNKNOWN_PLAYER";

    private static final String TAG = SqueezerRemoteControlConfigureActivity.class.getName();

    private static final String PREFS_NAME = "uk.org.ngo.squeezer.homescreenwidgets.SqueezerRemoteControl";
    private static final String PREF_PREFIX_KEY = "squeezerRemote_";
    private static final String PREF_SUFFIX_PLAYER_ID = "playerId";
    private static final String PREF_SUFFIX_PLAYER_NAME = "playerName";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private String playerId;


    public SqueezerRemoteControlConfigureActivity() {
        super();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadPlayerId(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_SUFFIX_PLAYER_ID, UNKNOWN_PLAYER);
    }

    static String loadPlayerName(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_SUFFIX_PLAYER_NAME, UNKNOWN_PLAYER);
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + PREF_SUFFIX_PLAYER_ID);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + PREF_SUFFIX_PLAYER_NAME);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.squeezer_remote_control_configure);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        playerId = loadPlayerId(SqueezerRemoteControlConfigureActivity.this, mAppWidgetId);
    }

    public PlayerBaseView createPlayerView() {
        return new PlayerBaseView<SqueezerRemoteControlConfigureActivity>(this, R.layout.list_item_player_simple) {
            public void onItemSelected(View view, int index, Player item) {
                super.onItemSelected(view, index, item);
            }

            public void onGroupSelected(View view, Player[] items) {

                final Context context = SqueezerRemoteControlConfigureActivity.this;

                // Write the prefix to the SharedPreferences object for this widget

                SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
                Player player = items[0];
                prefs.putString(PREF_PREFIX_KEY + mAppWidgetId + PREF_SUFFIX_PLAYER_ID, player.getId());
                prefs.putString(PREF_PREFIX_KEY + mAppWidgetId + PREF_SUFFIX_PLAYER_NAME, player.getName());
                prefs.apply();


                // It is the responsibility of the configuration activity to update the app widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                SqueezerRemoteControl.updateAppWidget(context, appWidgetManager, mAppWidgetId);

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        };
    }

    /*
    This Activity leverages a base Activity which almost all of squeezer uses, itself adding a
    player status, which we don't want on this activity.
     */
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);

        setListView(setupListView((AbsListView) findViewById(R.id.item_list)));
    }

    /*
    This Activity leverages a base Activity which almost all of squeezer uses, itself adding an
    actionBar, which we don't want on this activity.
     */
    protected boolean addActionBar() {
        return false;
    }

    @Override
    protected boolean needPlayer() {
        return false;
    }
}

