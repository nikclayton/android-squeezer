package uk.org.ngo.squeezer.service;

public enum ServerString {
    ALBUM,
    BROWSE_NEW_MUSIC,
    SORT_ARTISTYEARALBUM,
    SORT_ARTISTALBUM,
    SORT_YEARALBUM,
    SORT_YEARARTISTALBUM,
    REPEAT_OFF,
    REPEAT_ONE,
    REPEAT_ALL,
    SHUFFLE_OFF,
    SHUFFLE_ON_SONGS,
    SHUFFLE_ON_ALBUMS,
    SWITCH_TO_EXTENDED_LIST,
    SWITCH_TO_GALLERY,
    ALBUM_DISPLAY_OPTIONS,
    SLEEP,
    SLEEP_CANCEL,
    X_MINUTES,
    SLEEPING_IN,
    SLEEP_AT_END_OF_SONG,
    VOLUME,
    ;

    private String localizedString;

    /**
     * @return The localized string or just the name of the token, if not yet fetched (unlikely).
     */
    public String getLocalizedString() {
        return localizedString != null ? localizedString : name();
    }

    /**
     * Set the localized string for this token, as fetched from the server.
     *
     * @param localizedString
     */
    public void setLocalizedString(String localizedString) {
        this.localizedString = localizedString;
    }
}
