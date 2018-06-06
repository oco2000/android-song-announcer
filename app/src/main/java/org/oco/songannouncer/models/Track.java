package org.oco.songannouncer.models;

import android.text.TextUtils;

public class Track {

    private String playerPackageName;
    private String track;
    private String artist;
    private String album;
    private long duration;
    private long timestamp;

    public String getPlayerPackageName() {
        return playerPackageName;
    }

    public void setPlayerPackageName(String playerPackageName) {
        this.playerPackageName = playerPackageName;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Track: playerPackageName - " + playerPackageName +
                ", track name: " + track +
                ", artist: " + artist +
                ", album: " + album +
                ", duration: " + duration;
    }

    public String format(String fmt) {
        fmt = fmt.replaceAll("%T", getTrack());
        fmt = fmt.replaceAll("%A", getArtist());
        fmt = fmt.replaceAll("%a", getAlbum());
        fmt = fmt.replaceAll("%d", "" + getDuration());

        return fmt;
    }

    public Track copy() {
        final Track trackCopy = new Track();

        trackCopy.playerPackageName = playerPackageName;
        trackCopy.track = track;
        trackCopy.artist = artist;
        trackCopy.album = album;
        trackCopy.duration = duration;
        trackCopy.timestamp = timestamp;

        return trackCopy;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Track) {
            Track track = (Track) o;

            return TextUtils.equals(playerPackageName, track.playerPackageName)
                    && TextUtils.equals(this.track, track.track)
                    && TextUtils.equals(artist, track.artist)
                    && TextUtils.equals(album, track.album)
                    && duration == track.duration
                    && timestamp == track.timestamp;
        }

        return false;
    }
}
