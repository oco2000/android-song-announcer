package org.oco.songannouncer.service;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONObject;

import org.oco.songannouncer.util.Loggi;
import org.oco.songannouncer.util.IntentUtil;
import org.oco.songannouncer.util.AsyncTaskExecutor;
import org.oco.songannouncer.models.Track;
import org.oco.songannouncer.receiver.music.CommonMusicAppReceiver;

/* import com.artemzin.android.wail.R;
import com.artemzin.android.wail.api.lastfm.LFApiException;
import com.artemzin.android.wail.api.lastfm.LFTrackApi;
import com.artemzin.android.wail.api.lastfm.model.request.LFTrackRequestModel;
import com.artemzin.android.wail.api.lastfm.model.response.LFScrobbleResponseModel;
import com.artemzin.android.wail.api.network.NetworkException;
import com.artemzin.android.wail.notifications.SoundNotificationsManager;
import com.artemzin.android.wail.notifications.StatusBarNotificationsManager;
import com.artemzin.android.wail.storage.WAILSettings;
import com.artemzin.android.wail.storage.db.IgnoredPlayersDBHelper;
import com.artemzin.android.wail.storage.db.LovedTracksDBHelper;
import com.artemzin.android.wail.storage.db.TracksDBHelper;
import com.artemzin.android.wail.storage.model.Track;
import com.artemzin.android.wail.ui.activity.BaseActivity;
import com.artemzin.android.wail.ui.activity.WAILLoveWidget;
import com.artemzin.android.wail.util.AsyncTaskExecutor;
import com.artemzin.android.wail.util.IntentUtil;
import com.artemzin.android.wail.util.NetworkUtil;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SongAnnouncerService extends Service {


    public static final String INTENT_ACTION_HANDLE_TRACK = "INTENT_ACTION_HANDLE_TRACK";

    private static volatile Track lastUpdatedNowPlayingTrackInfo;

    private long lastScrobbleTime = 0;

    private Intent lastIntent;
    private String toast;

//    private IgnoredPlayersDBHelper ignoredPlayersDBHelper;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Loggi.i("WAILService onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Loggi.i("Service.onStartCommand() " + IntentUtil.getIntentAsString(intent));

        //ignoredPlayersDBHelper = IgnoredPlayersDBHelper.getInstance(getApplicationContext());

        if (intent == null) {
            // seems that system has recreated the service, if so
            // we should return START_STICKY
            return START_STICKY;
        }

        final String action = intent.getAction();

        if (action == null) {
            // null intent action
            return START_STICKY;
        }

        if (action.equals(INTENT_ACTION_HANDLE_TRACK)) {
            handleTrack(intent);
        } else {
            // unknown intent action
        }

        return START_STICKY;
    }

    private synchronized void updateNowPlaying(Track track) {
        if (track == null) {
            Loggi.w("WAILService.updateNowPlaying() track is null, skipping");
            return;
        }

        lastUpdatedNowPlayingTrackInfo = track.copy();
    }

    private void handleTrack(final Intent intent) {
/*
        if (intent == null || !WAILSettings.isEnabled(this)) {
            Loggi.w("WAILService track is not handled because WAIL is disabled");
            return;
        }
*/

        final String player = intent.getStringExtra(CommonMusicAppReceiver.EXTRA_PLAYER_PACKAGE_NAME);

/*
        if (ignoredPlayersDBHelper.contains(player)) {
            Loggi.w(String.format("WAILService track is not handled because the player %s is ignored", player));
            return;
        }
*/

        AsyncTaskExecutor.executeConcurrently(new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Loggi.i("\n\n----------\nWAILService track handling: " + intent);

                final String extraAction = intent.getStringExtra(CommonMusicAppReceiver.EXTRA_ACTION);

                if (extraAction == null || extraAction.lastIndexOf('.') == -1) {
                    Loggi.e("Can not handle track without player package name");
                    return null;
                }

                final boolean isCurrentTrackPlaying = intent.getBooleanExtra(CommonMusicAppReceiver.EXTRA_PLAYING, false);
                final Track currentTrack = CommonMusicAppReceiver.parseFromIntentExtras(intent);

                if (isCurrentTrackPlaying) {
                    updateNowPlaying(currentTrack);
                } else {
                    lastUpdatedNowPlayingTrackInfo = null;
//                    updateWidget(null);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (lastUpdatedNowPlayingTrackInfo != null) {
                    String text = lastUpdatedNowPlayingTrackInfo.getTrack() + " by " + lastUpdatedNowPlayingTrackInfo.getArtist();
                    Toast.makeText(SongAnnouncerService.this, text, Toast.LENGTH_LONG).show();
                }
            }

        });
    }

    public static class LastCapturedTrackInfo {

        private Track track;
        private boolean isPlaying;

        public LastCapturedTrackInfo(Track track, boolean isPlaying) {
            this.track = track;
            this.isPlaying = isPlaying;
        }

        public Track getTrack() {
            return track;
        }

        public boolean isPlaying() {
            return isPlaying;
        }

        public String toJSON() {
            final JSONObject json = new JSONObject();

            try {
                json.put("playerPackageName", track.getPlayerPackageName());
                json.put("track", track.getTrack());
                json.put("artist", track.getArtist());
                json.put("album", track.getAlbum());
                json.put("duration", track.getDuration());
                json.put("timestamp", track.getTimestamp());
                json.put("state", track.getState());
                json.put("stateTimestamp", track.getStateTimestamp());
                json.put("isPlaying", isPlaying);
            } catch (Exception e) {
                return null;
            }

            return json.toString();
        }

        public static LastCapturedTrackInfo fromJSON(String jsonString) {
            try {
                final JSONObject json = new JSONObject(jsonString);
                final Track track = new Track();

                track.setPlayerPackageName(json.optString("playerPackageName"));
                track.setTrack(json.optString("track"));
                track.setArtist(json.optString("artist"));
                track.setAlbum(json.optString("album"));
                track.setDuration(json.optLong("duration"));
                track.setTimestamp(json.optLong("timestamp"));
                track.setState(json.optInt("state"));
                track.setStateTimestamp(json.optLong("stateTimestamp"));

                return new LastCapturedTrackInfo(track, json.optBoolean("isPlaying"));
            } catch (Exception e) {
                return null;
            }
        }
    }

}
