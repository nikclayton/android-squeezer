package uk.org.ngo.squeezer.service;


/**
 * Interface to enable automatic removal of callbacks without the need for the
 * programmer to manually unregister the callback.
 * <p>
 * All callbacks must specify the context (usually Activity or Fragment in which
 * they run, so they can be unregistered via the Android life cycle methods.
 */
public interface ServiceCallback {

    /**
     * @return The context in which the callback runs
     */
    Object getClient();
}
