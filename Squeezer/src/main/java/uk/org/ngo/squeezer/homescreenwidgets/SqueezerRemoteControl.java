package uk.org.ngo.squeezer.homescreenwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link SqueezerRemoteControlConfigureActivity SqueezerRemoteControlConfigureActivity}
 */
public class SqueezerRemoteControl extends AppWidgetProvider {

    private static final String TAG = SqueezerRemoteControl.class.getName();


    private static final String SQUEEZER_REMOTE_POWER = "squeezeRemotePower";
    private static final String SQUEEZER_REMOTE_PAUSE_PLAY = "squeezeRemotePausePlay";
    public static final String PLAYER_ID = "playerId";

    private ISqueezeService service = null;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        String playerId = SqueezerRemoteControlConfigureActivity.loadPlayerId(context, appWidgetId);
        String playerName = SqueezerRemoteControlConfigureActivity.loadPlayerName(context, appWidgetId);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.squeezer_remote_control);

        views.setTextViewText(R.id.squeezerRemote_playerName, playerName);
        views.setOnClickPendingIntent(R.id.squeezerRemote_power, getPendingSelfIntent(context, SQUEEZER_REMOTE_POWER, playerId));
        views.setOnClickPendingIntent(R.id.squeezerRemote_pausePlay, getPendingSelfIntent(context, SQUEEZER_REMOTE_PAUSE_PLAY, playerId));
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static PendingIntent getPendingSelfIntent(Context context, String action, String playerId) {
        Intent intent = new Intent(context, SqueezerRemoteControl.class);
        intent.setAction(action);
        intent.putExtra(PLAYER_ID, playerId);

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public void runOnService(final Context context, final ServiceHandler handler) {

        IBinder service = peekService(context, new Intent(context, SqueezeService.class));

        if (service != null) {
            Log.i(TAG, "servicePeek found ISqueezeService");
            handler.run((ISqueezeService) service);
        } else {

            boolean bound = context.getApplicationContext().bindService(new Intent(context, SqueezeService.class), new ServiceConnection() {
                public void onServiceConnected(ComponentName name, IBinder service1) {
                    if (name != null && service1 instanceof ISqueezeService) {
                        Log.i(TAG, "onServiceConnected connected to ISqueezeService");
                        final ISqueezeService squeezeService = (ISqueezeService) service1;

                        // Might already be connected, try first
                        if (!runHandlerAndCatchException(handler, squeezeService)) {
                            Log.i(TAG, "ISqueezeService probably wasn't connected, connecting...");
                            // Probably wasn't connected. Connect and try again
                            squeezeService.getEventBus().register(new Object() {
                                public void onEvent(Object event) {
                                    if (event instanceof PlayerStateChanged) {
                                        Log.i(TAG, "Reconnected, trying again");
                                        runHandlerAndCatchException(handler, squeezeService);
                                        squeezeService.getEventBus().unregister(this);
                                    }
                                }
                            });
                            squeezeService.startConnect();
                        }
                        context.unbindService(this);
                    }
                }

                public void onServiceDisconnected(ComponentName name) {

                }
            }, Context.BIND_AUTO_CREATE);

            if (!bound)
                Log.e(TAG, "Squeezer service not bound");
        }
    }

    private boolean runHandlerAndCatchException(ServiceHandler handler, ISqueezeService squeezeService) {
        try {
            return handler.run(squeezeService);
        } catch (Exception ex) {
            Log.e(TAG, "Exception while handling serviceHandler", ex);
        }
        return false;
    }

    public void runOnPlayer(final Context context, final String playerId, final ServicePlayerHandler handler) {
        runOnService(context, new ServiceHandler() {
            public boolean run(ISqueezeService service) {
                for (Player player : service.getPlayers()) {
                    if (player.getId().equals(playerId)) {
                        handler.run(service, player);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private interface ServiceHandler {
        boolean run(ISqueezeService service);
    }

    private interface ServicePlayerHandler {
        void run(ISqueezeService service, Player player);
    }

    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        final String playerId = intent.getStringExtra(PLAYER_ID);

        if (SQUEEZER_REMOTE_POWER.equals(action)) {

            runOnPlayer(context, playerId, new ServicePlayerHandler() {
                public void run(ISqueezeService s, Player p) {
                    s.togglePower(p);
                }
            });
        }
        if (SQUEEZER_REMOTE_PAUSE_PLAY.equals(action)) {

            runOnPlayer(context, playerId, new ServicePlayerHandler() {
                public void run(ISqueezeService s, Player p) {
                    s.togglePausePlay(p);
                }
            });
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            SqueezerRemoteControlConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {


//		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
//		ComponentName thisAppWidget = new ComponentName(context.getPackageName(), SqueezerRemoteControl.class.getName());
//		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
//		onUpdate(context, appWidgetManager, appWidgetIds);

    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

