/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.service;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;

public class ConnectionState {

    private static final String TAG = "ConnectionState";

    /** {@link java.util.regex.Pattern} that splits strings on semi-colons. */
    private static final Pattern mSemicolonSplitPattern = Pattern.compile(";");

    // Incremented once per new connection and given to the Thread
    // that's listening on the socket.  So if it dies and it's not the
    // most recent version, then it's expected.  Else it should notify
    // the server of the disconnection.
    private final AtomicInteger currentConnectionGeneration = new AtomicInteger(0);

    // Connection state machine
    @IntDef({DISCONNECTED, CONNECTION_STARTED, CONNECTION_FAILED, CONNECTION_COMPLETED,
            LOGIN_STARTED, LOGIN_FAILED, LOGIN_COMPLETED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionStates {}
    /** Ordinarily disconnected from the server. */
    public static final int DISCONNECTED = 0;
    /** A connection has been started. */
    public static final int CONNECTION_STARTED = 1;
    /** The connection to the server did not complete. */
    public static final int CONNECTION_FAILED = 2;
    /** The connection to the server completed. */
    public static final int CONNECTION_COMPLETED = 3;
    /** The login process has started. */
    public static final int LOGIN_STARTED = 4;
    /** The login process has failed, the server is disconnected. */
    public static final int LOGIN_FAILED = 5;
    /** The login process completed, the handshake can start. */
    public static final int LOGIN_COMPLETED = 6;

    @ConnectionStates
    private volatile int mConnectionState = DISCONNECTED;

    /** Does the server support "favorites items" queries? */
    private final AtomicBoolean mCanFavorites = new AtomicBoolean(false);

    private final AtomicBoolean mCanMusicfolder = new AtomicBoolean(false);

    /** Does the server support "myapps items" queries? */
    private final AtomicBoolean mCanMyApps = new AtomicBoolean(false);

    private final AtomicBoolean canRandomplay = new AtomicBoolean(false);

    private final AtomicReference<String> preferredAlbumSort = new AtomicReference<String>("album");

    private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();

    private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();

    // Where we connected (or are connecting) to:
    private final AtomicReference<String> currentHost = new AtomicReference<String>();

    private final AtomicReference<Integer> httpPort = new AtomicReference<Integer>();

    private final AtomicReference<Integer> cliPort = new AtomicReference<Integer>();

    private final AtomicReference<String> userName = new AtomicReference<String>();

    private final AtomicReference<String> password = new AtomicReference<String>();

    private final AtomicReference<String[]> mediaDirs = new AtomicReference<String[]>();

    void disconnect(EventBus eventBus, boolean loginFailed) {
        Log.v(TAG, "disconnect" + (loginFailed ? ": authentication failure" : ""));
        currentConnectionGeneration.incrementAndGet();
        Socket socket = socketRef.get();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
        socketRef.set(null);
        socketWriter.set(null);

        if (loginFailed) {
            setConnectionState(eventBus, LOGIN_FAILED);
        } else {
            setConnectionState(eventBus, DISCONNECTED);
        }
        httpPort.set(null);
        mediaDirs.set(null);
    }

    /**
     * Sets a new connection state, and posts a sticky
     * {@link uk.org.ngo.squeezer.service.event.ConnectionChanged} event with the new state.
     *
     * @param eventBus The eventbus to post the event to.
     * @param connectionState The new connection state.
     */
    void setConnectionState(@NonNull EventBus eventBus,
            @ConnectionStates int connectionState) {
        Log.d(TAG, "Setting connection state to: " + connectionState);
        mConnectionState = connectionState;
        eventBus.postSticky(new ConnectionChanged(mConnectionState));
    }

    PrintWriter getSocketWriter() {
        return socketWriter.get();
    }

    void setHttpPort(Integer port) {
        httpPort.set(port);
        Log.v(TAG, "HTTP port is now: " + port);
    }

    public String[] getMediaDirs() {
        String[] dirs = mediaDirs.get();
        return dirs == null ? new String[0] : dirs;
    }

    public void setMediaDirs(String dirs) {
        mediaDirs.set(mSemicolonSplitPattern.split(dirs));
    }

    void setCanFavorites(boolean value) {
        mCanFavorites.set(value);
    }

    boolean canFavorites() {
        return mCanFavorites.get();
    }

    void setCanMusicfolder(boolean value) {
        mCanMusicfolder.set(value);
    }

    boolean canMusicfolder() {
        return mCanMusicfolder.get();
    }

    void setCanMyApps(boolean value) {
        mCanMyApps.set(value);
    }

    boolean canMyApps() {
        return mCanMyApps.get();
    }

    void setCanRandomplay(boolean value) {
        canRandomplay.set(value);
    }

    boolean canRandomplay() {
        return canRandomplay.get();
    }

    public void setPreferedAlbumSort(String value) {
        preferredAlbumSort.set(value);
    }

    public String getPreferredAlbumSort() {
        return preferredAlbumSort.get();
    }

    /**
     * @return True if the socket connection to the server has completed.
     */
    boolean isConnected() {
        switch (mConnectionState) {
            case CONNECTION_COMPLETED:
            case LOGIN_STARTED:
            case LOGIN_COMPLETED:
                return true;

            default:
                return false;
        }
    }

    /**
     * @return True if the socket connection to the server has started, but not yet
     *     completed (successfully or unsuccessfully).
     */
    boolean isConnectInProgress() {
        return mConnectionState == CONNECTION_STARTED;
    }

    void startListeningThread(@NonNull EventBus eventBus, @NonNull Executor executor, CliClient cli) {
        Thread listeningThread = new ListeningThread(eventBus, executor, cli, socketRef.get(),
                currentConnectionGeneration.incrementAndGet());
        listeningThread.start();
    }

    private class ListeningThread extends Thread {

        @NonNull private final EventBus mEventBus;

        @NonNull private final Executor mExecutor;

        private final Socket socket;

        private final CliClient cli;

        private final int generationNumber;

        private ListeningThread(@NonNull EventBus eventBus, @NonNull Executor executor, CliClient cli, Socket socket, int generationNumber) {
            mEventBus = eventBus;
            mExecutor = executor;
            this.cli = cli;
            this.socket = socket;
            this.generationNumber = generationNumber;
        }

        @Override
        public void run() {
            Log.d(TAG, "Listening thread started");
            BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()), 128);
            } catch (IOException e) {
                Log.v(TAG, "IOException while creating BufferedReader: " + e);
                cli.disconnect(false);
                return;
            }
            IOException exception = null;
            while (true) {
                String line;
                try {
                    line = in.readLine();
                } catch (IOException e) {
                    line = null;
                    exception = e;
                }
                if (line == null) {
                    // Socket disconnected.  This is expected
                    // if we're not the main connection generation anymore,
                    // else we should notify about it.
                    if (currentConnectionGeneration.get() == generationNumber) {
                        Log.v(TAG, "Server disconnected; exception=" + exception);
                        cli.disconnect(exception == null);
                    } else {
                        // Who cares.
                        Log.v(TAG, "Old generation connection disconnected, as expected.");
                    }
                    return;
                }
                final String inputLine = line;

                // If a login attempt was in progress and this is a line that does not start
                // with "login " then the login must have been successful (otherwise the
                // server would have disconnected), so update the connection state accordingly.
                if (mConnectionState == LOGIN_STARTED && !inputLine.startsWith("login ")) {
                    setConnectionState(mEventBus, LOGIN_COMPLETED);
                }
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        cli.onLineReceived(inputLine);
                    }
                });
            }
        }
    }

    void startConnect(final SqueezeService service, @NonNull final EventBus eventBus,
                      @NonNull final Executor executor,
                      final CliClient cli, String hostPort, final String userName,
                      final String password) {
        Log.v(TAG, "startConnect");
        // Common mistakes, based on crash reports...
        if (hostPort.startsWith("Http://") || hostPort.startsWith("http://")) {
            hostPort = hostPort.substring(7);
        }

        // Ending in whitespace?  From LatinIME, probably?
        while (hostPort.endsWith(" ")) {
            hostPort = hostPort.substring(0, hostPort.length() - 1);
        }

        final int port = Util.parsePort(hostPort);
        final String host = Util.parseHost(hostPort);
        final String cleanHostPort = host + ":" + port;

        currentHost.set(host);
        cliPort.set(port);
        httpPort.set(null);  // not known until later, after connect.
        this.userName.set(userName);
        this.password.set(password);

        // Start the off-thread connect.
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Ensuring service is disconnected");
                service.disconnect();
                Socket socket = new Socket();
                try {
                    Log.d(TAG, "Connecting to: " + cleanHostPort);
                    setConnectionState(eventBus, CONNECTION_STARTED);
                    socket.connect(new InetSocketAddress(host, port),
                            4000 /* ms timeout */);
                    socketRef.set(socket);
                    Log.d(TAG, "Connected to: " + cleanHostPort);
                    socketWriter.set(new PrintWriter(socket.getOutputStream(), true));
                    setConnectionState(eventBus, CONNECTION_COMPLETED);
                    startListeningThread(eventBus, executor, cli);
                    onCliPortConnectionEstablished(eventBus, cli, userName, password);
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(userName, password.toCharArray());
                        }
                    });
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Socket timeout connecting to: " + cleanHostPort);
                    setConnectionState(eventBus, CONNECTION_FAILED);
                } catch (IOException e) {
                    Log.e(TAG, "IOException connecting to: " + cleanHostPort);
                    setConnectionState(eventBus, CONNECTION_FAILED);
                }
            }

        });
    }

    /**
     * Authenticate on the SqueezeServer.
     * <p/>
     * The server does
     * <pre>
     * login user wrongpassword
     * login user ******
     * (Connection terminated)
     * </pre>
     * instead of as documented
     * <pre>
     * login user wrongpassword
     * (Connection terminated)
     * </pre>
     * therefore a disconnect when handshake (the next step after authentication) is not completed,
     * is considered an authentication failure.
     */
    void onCliPortConnectionEstablished(final EventBus eventBus, final CliClient cli, final String userName, final String password) {
        setConnectionState(eventBus, ConnectionState.LOGIN_STARTED);
        cli.sendCommandImmediately("login " + Util.encode(userName) + " " + Util.encode(password));
    }


    Integer getHttpPort() {
        return httpPort.get();
    }

    String getUserName() {
        return userName.get();
    }

    String getPassword() {
        return password.get();
    }

    String getCurrentHost() {
        return currentHost.get();
    }

}
