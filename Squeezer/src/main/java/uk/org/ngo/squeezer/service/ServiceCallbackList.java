package uk.org.ngo.squeezer.service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a list of
 */
public class ServiceCallbackList<T extends ServiceCallback> implements Iterable<T> {
    private final ServicePublisher publisher;
    private final Map<T, Boolean> items = new ConcurrentHashMap<T, Boolean>();

    public ServiceCallbackList(ServicePublisher publisher) {
        this.publisher = publisher;
    }

    public int count() {
        return items.size();
    }

    public ServiceCallbackList<T> register(T item) {
        publisher.addClient(this, item);
        items.put(item, Boolean.TRUE);
        return this;
    }

    public ServiceCallbackList<T> unregister(T item) {
        publisher.removeClient(item);
        items.remove(item);
        return this;
    }

    @Override
    public Iterator<T> iterator() {
        return items.keySet().iterator();
    }

    public interface ServicePublisher {
        void addClient(ServiceCallbackList callbackList, ServiceCallback item);
        void removeClient(ServiceCallback item);
    }
}
