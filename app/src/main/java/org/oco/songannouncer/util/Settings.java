package org.oco.songannouncer.util;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {
    // region keys
    private static final String KEY_ENABLE_MESSAGES                = "enable_messages";
    private static final String KEY_MESSAGE_FORMAT                 = "message_format";
    private static final String KEY_ENABLE_SPEECH                  = "enable_speech";
    private static final String KEY_SPEECH_FORMAT                  = "speech_format";
    private static final String KEY_SPEECH_LANGUAGE                = "speech_language";
    // endregion

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized String getLanguage(Context context) {
        return getSharedPreferences(context).getString(KEY_SPEECH_LANGUAGE, "en-US");
    }

    public static synchronized boolean isMessagesEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_ENABLE_MESSAGES, true);
    }

    public static synchronized String getMessageFormat(Context context) {
        return getSharedPreferences(context).getString(KEY_MESSAGE_FORMAT, "%T by %A");
    }

    public static synchronized boolean isSpeechEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_ENABLE_SPEECH, true);
    }

    public static synchronized String getSpeechFormat(Context context) {
        return getSharedPreferences(context).getString(KEY_SPEECH_FORMAT, "Now playing %T by %A");
    }


}
