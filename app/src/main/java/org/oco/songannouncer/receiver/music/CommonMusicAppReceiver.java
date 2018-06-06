package org.oco.songannouncer.receiver.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.oco.songannouncer.service.SongAnnouncerService;
import org.oco.songannouncer.util.AsyncTaskExecutor;
import org.oco.songannouncer.util.IntentUtil;
import org.oco.songannouncer.util.Loggi;
import org.oco.songannouncer.models.Track;

public abstract class CommonMusicAppReceiver extends BroadcastReceiver {

    public static final String EXTRA_ACTION              = "EXTRA_ACTION";
    public static final String EXTRA_PLAYER_PACKAGE_NAME = "EXTRA_PLAYER_PACKAGE_NAME";
    public static final String EXTRA_ID                  = "EXTRA_ID";
    public static final String EXTRA_PLAYING             = "EXTRA_PLAYING";
    public static final String EXTRA_ALBUM_ID            = "EXTRA_ALBUM_ID";
    public static final String EXTRA_TRACK               = "EXTRA_TRACK";
    public static final String EXTRA_ARTIST              = "EXTRA_ARTIST";
    public static final String EXTRA_ALBUM               = "EXTRA_ALBUM";
    public static final String EXTRA_DURATION            = "EXTRA_DURATION";

    public static final String EXTRA_TIMESTAMP           = "EXTRA_TIMESTAMP";

    @Override
    public final void onReceive(Context context, Intent intent) {
        asyncProcessTheIntent(context, intent);
    }

    protected final Intent newIntentForService(Context context) {
        return new Intent(context, SongAnnouncerService.class);
    }

    private void asyncProcessTheIntent(final Context context, final Intent intent) {
        AsyncTaskExecutor.executeConcurrently(new AsyncTask<Void, Void, Intent>() {
            @Override
            protected Intent doInBackground(Void... params) {
                try {
                    try {
                        Loggi.d("CommonMusicAppReceiver.onReceive() intent: " + IntentUtil.getIntentAsString(intent));
                    } catch (Exception e) {
                        Loggi.e("CommonMusicAppReceiver.onReceive() can not display intent info");
                    }

                    if (intent == null) {
                        Loggi.e("CommonMusicAppReceiver.onReceive() intent is null");
                        return null;
                    }

                    if (TextUtils.isEmpty(intent.getAction())
                            || intent.getAction().indexOf('.') == -1) {
                        Loggi.e("CommonMusicAppReceiver.onReceive() intent action is corrupted: " + intent.getAction());
                        return null;
                    }

                    if (intent.getExtras() == null || intent.getExtras().size() == 0) {
                        Loggi.e("CommonMusicAppReceiver.onReceive() intent extras are null or empty, skipping intent");
                        return null;
                    }

                    if (isInitialStickyBroadcast()) {
                        Loggi.w("CommonMusicAppReceiver.onReceive() received cached sticky broadcast, the App won't process it");
                        return null;
                    }

                    final Intent intentForService = handleIntent(context, intent);

                    if (intentForService != null) {
                        intentForService.setAction(SongAnnouncerService.INTENT_ACTION_HANDLE_TRACK);
                        intentForService.putExtra(EXTRA_ACTION, intent.getAction());
                        intentForService.putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis());

                        return intentForService;
                    } else {
                        Loggi.w("CommonMusicAppReceiver.onReceive() did not send intent for service, handleIntent() returns null, skipping intent");
                        return null;
                    }
                } catch (Exception e) {

                    final String log = "CommonMusicAppReceiver.onReceive() exception while handleIntent(): " + e.toString();
                    Loggi.e(log);

                    return null;
                }
            }

            @Override
            protected void onPostExecute(Intent intentForService) {
                if (intentForService != null) {
                    context.startService(intentForService);
                }
            }
        });
    }

    protected Intent handleIntent(Context context, Intent originalIntent) {
        final Intent handleTrackIntent = newIntentForService(context);

        handleTrackIntent.putExtra(EXTRA_PLAYER_PACKAGE_NAME, originalIntent.getAction());

        handleTrackIntent.putExtra(EXTRA_ID, IntentUtil.getLongOrIntExtra(originalIntent, -1, "id", "trackid", "trackId"));

        Boolean isPlaying = false;

        if (originalIntent.getAction() == "com.adam.aslfms.notify.playstatechanged") {
            int state = originalIntent.getIntExtra("state", 2);
            isPlaying = state < 2;
        } else {
            isPlaying = IntentUtil.getBoolOrNumberAsBoolExtra(originalIntent, null, "playing", "playstate", "isPlaying", "isplaying", "is_playing");
        }

        if (isPlaying == null) {
            Loggi.w("CommonMusicAppReceiver track info does not contains playing state, ignoring");
            return null;
        } else {
            handleTrackIntent.putExtra(EXTRA_PLAYING, isPlaying);
        }

        handleTrackIntent.putExtra(EXTRA_ALBUM_ID, IntentUtil.getLongOrIntExtra(originalIntent, -1, "albumid", "albumId"));
        handleTrackIntent.putExtra(EXTRA_TRACK,    originalIntent.getStringExtra("track"));
        handleTrackIntent.putExtra(EXTRA_ARTIST,   originalIntent.getStringExtra("artist"));
        handleTrackIntent.putExtra(EXTRA_ALBUM,    originalIntent.getStringExtra("album"));

        long duration = IntentUtil.getLongOrIntExtra(originalIntent, -1, "duration");

        if (duration != -1) {
            if (duration < 30000) { // it is in seconds, we should convert it to millis
                duration *= 1000;
            }
        }

        handleTrackIntent.putExtra(EXTRA_DURATION, duration);

        return handleTrackIntent;
    }


    public static Track parseFromIntentExtras(final Intent intent) {
        final Track track = new Track();

        track.setPlayerPackageName(intent.getStringExtra(EXTRA_PLAYER_PACKAGE_NAME));
        track.setTrack(intent.getStringExtra(EXTRA_TRACK));
        track.setArtist(intent.getStringExtra(EXTRA_ARTIST));
        track.setAlbum(intent.getStringExtra(EXTRA_ALBUM));
        track.setDuration(intent.getLongExtra(EXTRA_DURATION, -1L));
        track.setTimestamp(intent.getLongExtra(EXTRA_TIMESTAMP, -1L));

        return track;
    }
}
