
package uk.org.ngo.squeezer.test.server;

import android.content.Intent;
import android.test.ServiceTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.service.ConnectionState;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.test.mock.SqueezeboxServerMock;

/**
 * To test interactions with the server:
 * <ol>
 *     <li>Create a mock server configured appropriately for the test.</li>
 *     <li>Set a desired
 *     {@link uk.org.ngo.squeezer.service.ConnectionState.ConnectionStates} in
 *     {@link #mWantedState}.</li>
 *     <li>Connect to the mock server.</li>
 *     <li>Wait on {@link #mLockWantedState}. Execution will continue when the server
 *     reaches the same state as is in {@link #mWantedState}.</li>
 *     <li>At this point {@link #mActualConnectionStates} contains an ordered list of the
 *     connection state transitions the service has been through.</li>
 * </ol>
 */
public class SqueezeServiceTest extends ServiceTestCase<SqueezeService> {
    private static final String TAG = "SqueezeServiceTest";

    /** Number of milliseconds to wait for a particular event to occur before aborting. */
    private static final int TIMEOUT_IN_MS = 5000;

    public SqueezeServiceTest() {
        super(SqueezeService.class);
    }

    /** Wait until the server has reached this state. */
    @ConnectionState.ConnectionStates
    private int mWantedState;

    /**
     * Lock object, will be notified when the server reaches the same state as
     * {@link #mWantedState}.
     */
    private final Object mLockWantedState = new Object();

    /** List of the states the server has transition through. */
    private final List<Integer> mActualConnectionStates = new ArrayList<Integer>();

    /**
     * Lock object, will be notified when the service completes the handshake with the
     * server.
     */
    private final Object mLockHandshakeComplete = new Object();

    /** The last successful handshake-complete event. */
    private HandshakeComplete mLastHandshakeCompleteEvent;

    private ISqueezeService mService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mService = (ISqueezeService) bindService(new Intent(getSystemContext(), SqueezeService.class));
        mService.getEventBus().register(this);
    }

    protected void tearDown() throws Exception {
        mService.getEventBus().unregister(this);
        shutdownService();
        super.tearDown();
    }

    /**
     * Verify that connecting to a non-existent server fails.
     *
     * @throws InterruptedException
     */
    public void testConnectionFailure() throws InterruptedException {
        SqueezeboxServerMock.starter().start();
        mWantedState = ConnectionState.CONNECTION_FAILED;

        mService.startConnect("localhost", "test", "test");

        synchronized(mLockWantedState) {
            mLockWantedState.wait(TIMEOUT_IN_MS);
        }

        assertEquals(Arrays.asList(
                ConnectionState.DISCONNECTED,
                ConnectionState.CONNECTION_STARTED,
                ConnectionState.CONNECTION_FAILED), mActualConnectionStates);
    }

    /**
     * Verify that connecting to an existing server succeeds.
     *
     * @throws InterruptedException
     */
    public void testConnect() throws InterruptedException {
        SqueezeboxServerMock.starter().start();

        mService.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT,
                "test", "test");

        synchronized (mLockHandshakeComplete) {
            mLockHandshakeComplete.wait(TIMEOUT_IN_MS);
        }

        assertEquals(Arrays.asList(
                ConnectionState.DISCONNECTED,
                ConnectionState.CONNECTION_STARTED,
                ConnectionState.CONNECTION_COMPLETED,
                ConnectionState.LOGIN_STARTED,
                ConnectionState.LOGIN_COMPLETED), mActualConnectionStates);

        assertTrue(mLastHandshakeCompleteEvent.canMusicFolders);
        assertTrue(mLastHandshakeCompleteEvent.canRandomPlay);
        assertEquals(AlbumViewDialog.AlbumsSortOrder.album.name(),
                mService.preferredAlbumSort());

        // Check that disconnecting only generates one additional DISCONNECTED event.
        mService.disconnect();

        assertEquals(Arrays.asList(
                ConnectionState.DISCONNECTED,
                ConnectionState.CONNECTION_STARTED,
                ConnectionState.CONNECTION_COMPLETED,
                ConnectionState.LOGIN_STARTED,
                ConnectionState.LOGIN_COMPLETED,
                ConnectionState.DISCONNECTED), mActualConnectionStates);
    }

    /**
     * Verify that connecting to an existing server that uses password authentication,
     * using the correct password, succeeds.
     *
     * @throws InterruptedException
     */
    public void testConnectProtectedServer() throws InterruptedException {
        SqueezeboxServerMock.starter().username("user").password("1234").start();

        mService.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT,
                "user", "1234");

        synchronized (mLockHandshakeComplete) {
            mLockHandshakeComplete.wait(TIMEOUT_IN_MS);
        }

        assertEquals(Arrays.asList(
                ConnectionState.DISCONNECTED,
                ConnectionState.CONNECTION_STARTED,
                ConnectionState.CONNECTION_COMPLETED,
                ConnectionState.LOGIN_STARTED,
                ConnectionState.LOGIN_COMPLETED), mActualConnectionStates);

        assertTrue(mLastHandshakeCompleteEvent.canMusicFolders);
        assertTrue(mLastHandshakeCompleteEvent.canRandomPlay);
        assertEquals(AlbumViewDialog.AlbumsSortOrder.album.name(),
                mService.preferredAlbumSort());

        // Check that disconnecting only generates one additional DISCONNECTED event.
        mService.disconnect();

        assertEquals(Arrays.asList(
                ConnectionState.DISCONNECTED,
                ConnectionState.CONNECTION_STARTED,
                ConnectionState.CONNECTION_COMPLETED,
                ConnectionState.LOGIN_STARTED,
                ConnectionState.LOGIN_COMPLETED,
                ConnectionState.DISCONNECTED), mActualConnectionStates);
    }

    /**
     * Verify that connecting to an existing server that uses password authentication,
     * using an incorrect username / password fails.
     *
     * @throws InterruptedException
     */
    public void testAuthenticationFailure() throws InterruptedException {
        SqueezeboxServerMock.starter().username("user").password("1234").start();
        mWantedState = ConnectionState.LOGIN_FAILED;

        mService.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT, "test", "test");

        synchronized (mLockWantedState) {
            mLockWantedState.wait(TIMEOUT_IN_MS);
        }

        assertEquals(Arrays.asList(
                ConnectionState.DISCONNECTED,
                ConnectionState.CONNECTION_STARTED,
                ConnectionState.CONNECTION_COMPLETED,
                ConnectionState.LOGIN_STARTED,
                ConnectionState.LOGIN_FAILED), mActualConnectionStates);
    }

    public void onEvent(ConnectionChanged event) {
        mActualConnectionStates.add(event.connectionState);

        // If the desired state is DISCONNECTED then ignore it the very first time it
        // appears, as it's the initial state.
        if (mWantedState == ConnectionState.DISCONNECTED && mActualConnectionStates.size() == 1) {
            return;
        }

        if (event.connectionState == mWantedState) {
            synchronized (mLockWantedState) {
                mLockWantedState.notify();
            }
        }
    }

    public void onEvent(HandshakeComplete event) {
        mLastHandshakeCompleteEvent = event;
        synchronized (mLockHandshakeComplete) {
            mLockHandshakeComplete.notify();
        }
    }
}
