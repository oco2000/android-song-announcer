package org.oco.songannouncer.service;

import java.util.Locale;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import org.oco.songannouncer.util.Settings;
import org.oco.songannouncer.util.Loggi;
import org.oco.songannouncer.util.IntentUtil;
import org.oco.songannouncer.util.AsyncTaskExecutor;
import org.oco.songannouncer.models.Track;
import org.oco.songannouncer.receiver.music.CommonMusicAppReceiver;

public class SongAnnouncerService extends Service {


    public static final String INTENT_ACTION_HANDLE_TRACK = "INTENT_ACTION_HANDLE_TRACK";

    private static volatile Track lastUpdatedNowPlayingTrackInfo;

    private TextToSpeech tts;

//    private IgnoredPlayersDBHelper ignoredPlayersDBHelper;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Loggi.i("Service onCreate()");

        tts=new TextToSpeech(SongAnnouncerService.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Loggi.e("This Language is not supported");
                    }
                }
                else
                    Loggi.e("TTS Initialization Failed!");
            }
        });

    }

    @Override
    public void onDestroy() {
        if(tts != null){
            tts.stop();
            tts.shutdown();
        }
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
            Loggi.w("Service.updateNowPlaying() track is null, skipping");
            return;
        }

        lastUpdatedNowPlayingTrackInfo = track.copy();
    }

    private void handleTrack(final Intent intent) {
/*
        if (intent == null || !Settings.isEnabled(this)) {
            Loggi.w("Service track is not handled because  is disabled");
            return;
        }
*/

//        final String player = intent.getStringExtra(CommonMusicAppReceiver.EXTRA_PLAYER_PACKAGE_NAME);

/*
        if (ignoredPlayersDBHelper.contains(player)) {
            Loggi.w(String.format("Service track is not handled because the player %s is ignored", player));
            return;
        }
*/

        AsyncTaskExecutor.executeConcurrently(new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Loggi.i("\n\n----------\nService track handling: " + intent);

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
                    String text;
                    if (Settings.isMessagesEnabled(SongAnnouncerService.this)) {
                        text = lastUpdatedNowPlayingTrackInfo.format(Settings.getMessageFormat(SongAnnouncerService.this));
                        Toast.makeText(SongAnnouncerService.this, text, Toast.LENGTH_LONG).show();
                    }

                    if (Settings.isSpeechEnabled(SongAnnouncerService.this) && tts != null) {
                        text = lastUpdatedNowPlayingTrackInfo.format(Settings.getSpeechFormat(SongAnnouncerService.this));
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
            }

        });
    }

}
