
package uk.org.ngo.squeezer.test.server;

import android.content.Intent;
import android.os.IBinder;
import android.test.ServiceTestCase;

import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog.AlbumsSortOrder;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.test.mock.SqueezeboxServerMock;

/**
 * @author Kurt Aaholst <kaaholst@gmail.com>
 */
public class SqueezeServiceTest extends ServiceTestCase<SqueezeService> {

    public SqueezeServiceTest() {
        super(SqueezeService.class);
    }

    public void testConnectionFailure() throws InterruptedException {
        IBinder binder = bindService(new Intent(getContext(), SqueezeService.class));
        ISqueezeService service = (ISqueezeService) binder;
        ServiceCallbackTest serviceCallback = new ServiceCallbackTest();

        service.registerConnectionCallback(serviceCallback);
        service.startConnect("localhost", "test", "test");
        Thread.sleep(1000); //TODO proper synchronization

        assertEquals(2, serviceCallback.onConnectionChanged);
        assertFalse(serviceCallback.isConnected);
    }

    public void testConnect() throws InterruptedException {
        IBinder binder = bindService(new Intent(getContext(), SqueezeService.class));
        ISqueezeService service = (ISqueezeService) binder;
        ServiceCallbackTest serviceCallback = new ServiceCallbackTest();
        WaitForHandshake waitForHandshake = new WaitForHandshake(service);

        SqueezeboxServerMock.starter().start();
        service.registerConnectionCallback(serviceCallback);
        service.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT, "test", "test");
        waitForHandshake.waitForHandshakeCompleted();

        assertEquals(2, serviceCallback.onConnectionChanged);
        assertTrue(serviceCallback.isConnected);
        assertTrue(service.canMusicfolder());
        assertTrue(service.canRandomplay());
        assertEquals(AlbumsSortOrder.album.name(), service.preferredAlbumSort());
    }

    public void testConnectProtectedServer() throws InterruptedException {
        IBinder binder = bindService(new Intent(getContext(), SqueezeService.class));
        ISqueezeService service =(ISqueezeService) binder;
        ServiceCallbackTest serviceCallback = new ServiceCallbackTest();

        SqueezeboxServerMock.starter().username("user").password("1234").start();

        service.registerConnectionCallback(serviceCallback);
        WaitForHandshake waitForHandshake = new WaitForHandshake(service);
        service.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT, "user", "1234");
        waitForHandshake.waitForHandshakeCompleted();

        assertEquals(2, serviceCallback.onConnectionChanged);
        assertTrue(serviceCallback.isConnected);
        assertTrue(service.canMusicfolder());
        assertTrue(service.canRandomplay());
        assertEquals(AlbumsSortOrder.album.name(), service.preferredAlbumSort());
    }

    public void testAuthenticationFailure() throws InterruptedException {
        IBinder binder = bindService(new Intent(getContext(), SqueezeService.class));
        ISqueezeService service = (ISqueezeService) binder;
        ServiceCallbackTest serviceCallback = new ServiceCallbackTest();

        SqueezeboxServerMock.starter().username("user").password("1234").start();

        service.registerConnectionCallback(serviceCallback);
        service.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT, "test", "test");
        Thread.sleep(1000); //TODO proper synchronization

        assertEquals(3, serviceCallback.onConnectionChanged);
        assertFalse(serviceCallback.isConnected);
    }

}
