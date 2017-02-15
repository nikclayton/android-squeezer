
package uk.org.ngo.squeezer.test.server;

import junit.framework.AssertionFailedError;

import uk.org.ngo.squeezer.service.IServiceHandshakeCallback;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class WaitForHandshake implements IServiceHandshakeCallback {

    private final Object handshakeMonitor = new Object();

    private boolean handshakeCompleted;

    public WaitForHandshake(ISqueezeService service) {
        service.registerHandshakeCallback(this);
    }

    @Override
    synchronized public void onHandshakeCompleted() {
        synchronized (handshakeMonitor) {
            handshakeCompleted = true;
            handshakeMonitor.notifyAll();
        }
    }

    public void waitForHandshakeCompleted() {
        synchronized (handshakeMonitor) {
            while (!handshakeCompleted) {
                try {
                    handshakeMonitor.wait(2000);
                    if (!handshakeCompleted) {
                        throw new AssertionFailedError("Expected handshake to complete");
                    }
                } catch (InterruptedException e) {
                    System.out.println("InterruptedException caught in waitForHandshakeCompleted");
                }
            }
        }
    }

    @Override
    public Object getClient() {
        return this;
    }
}
