package uk.org.ngo.squeezer.test.server;

import uk.org.ngo.squeezer.service.IServiceConnectionCallback;

public class ServiceCallbackTest implements IServiceConnectionCallback {

    int onConnectionChanged;

    boolean isConnected;

    boolean isPostConnect;

    boolean isLoginFailed;


    @Override
    public void onConnectionChanged(boolean isConnected, boolean postConnect, boolean loginFailed) {
        onConnectionChanged++;
        this.isConnected = isConnected;
        isPostConnect = postConnect;
        isLoginFailed = loginFailed;
    }

    @Override
    public Object getClient() {
        return this;
    }
}
