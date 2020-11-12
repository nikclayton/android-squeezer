package uk.org.ngo.squeezer.homescreenwidgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.PlayerBaseView;
import uk.org.ngo.squeezer.itemlist.PlayerListBaseActivity;
import uk.org.ngo.squeezer.model.Item;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;

/**
 * The configuration screen for the {@link SqueezerRemoteControl SqueezerRemoteControl} AppWidget.
 */
public class SqueezerRemoteControlPlayerSelectActivity extends PlayerListBaseActivity {

    private static final String TAG = SqueezerRemoteControlPlayerSelectActivity.class.getName();

    private static final int GET_BUTTON_ACTIVITY = 1001;

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;


    public SqueezerRemoteControlPlayerSelectActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Log.d(TAG, "onCreate");

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        // Actual result, when successful is below in the onGroupSelected handler
        setResult(RESULT_CANCELED);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.configure_select_player);
        }

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

    }

    public PlayerBaseView createPlayerView(View view) {
        return new SqueezerRemoteControlConfigureActivityPlayerView(view);
    }

    /*
    This Activity leverages a base Activity which almost all of squeezer uses, itself adding a
    player status, which we don't want on this activity.
     */
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        // TODO rcv implement method comment
        super.setContentView(layoutResID);
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

    @Override
    protected <T extends Item> void updateAdapter(int count, int start, List<T> items, Class<T> dataType) {

    }

    private class SqueezerRemoteControlConfigureActivityPlayerView extends PlayerBaseView<SqueezerRemoteControlPlayerSelectActivity> {

        public SqueezerRemoteControlConfigureActivityPlayerView(View view) {
            super(SqueezerRemoteControlPlayerSelectActivity.this, view);
            // TODO rcv where to put this R.layout.list_item_player_simple
            setViewParams(VIEW_PARAM_ICON);
        }

        @Override
        public void bindView(Player player) {
            super.bindView(player);
            icon.setImageResource(getModelIcon(player.getModel()));

            PlayerState playerState = player.getPlayerState();

            if (playerState.isPoweredOn()) {
                text1.setAlpha(1.0f);
            } else {
                text1.setAlpha(0.25f);
            }
        }

        // TODO rcv
        public void onGroupSelected(View view, Player[] items) {
            final Context context = SqueezerRemoteControlPlayerSelectActivity.this;

            Intent intent = new Intent(context, SqueezerRemoteControlButtonSelectActivity.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            intent.putExtra(SqueezerRemoteControl.EXTRA_PLAYER, items[0]);

            startActivityForResult(intent, GET_BUTTON_ACTIVITY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GET_BUTTON_ACTIVITY:
                if (resultCode != RESULT_CANCELED) {
                    SqueezerRemoteControl.savePrefs(this.getBaseContext(), data);

                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                }
                break;
            default:
                Log.w(TAG, "Unknown request code: " + requestCode);
        }
    }
}

