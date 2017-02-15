package uk.org.ngo.squeezer.test.mock;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog.AlbumsSortOrder;

/**
 * This is a test of the mock server.
 * <p/>
 * It doesn't really test any Squeezer functionally, but it is handy when developing or debugging
 * the mock server.
 *
 * @author Kurt Aaholst <kaaholst@gmail.com>
 */
public class SqueezeboxServerMockTest extends TestCase {

    public void testDefaults() {
        SqueezeboxServerMock.starter().start();

        try {
            SocketAddress sa = new InetSocketAddress("localhost", SqueezeboxServerMock.CLI_PORT);
            Socket socket = new Socket();

            socket.connect(sa, 10 * 1000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()),
                    128);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("login testuser testpassword");
            assertEquals("login testuser ******", in.readLine());

            out.println("abcd");
            assertEquals("abcd", in.readLine());

            out.println("version");
            assertEquals("version", in.readLine());

            out.println("can musicfolder ?");
            out.println("can randomplay ?");
            out.println("pref httpport ?");
            out.println("pref jivealbumsort ?");
            out.println("version ?");
            assertEquals("can musicfolder 1", in.readLine());
            assertEquals("can randomplay 1", in.readLine());
            assertEquals("pref httpport 9092", in.readLine());
            assertEquals("pref jivealbumsort album", in.readLine());
            assertEquals("version 7.7.2", in.readLine());

            out.println("exit");
            assertEquals("exit", in.readLine());

            assertNull(in.readLine());

            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testSettings() {
        SqueezeboxServerMock.starter().canRandomplay(false).albumsSortOrder(AlbumsSortOrder.artflow)
                .start();

        try {
            SocketAddress sa = new InetSocketAddress("localhost", SqueezeboxServerMock.CLI_PORT);
            Socket socket = new Socket();

            socket.connect(sa, 10 * 1000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()),
                    128);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("can musicfolder ?");
            out.println("can randomplay ?");
            out.println("pref httpport ?");
            out.println("pref jivealbumsort ?");
            out.println("version ?");
            assertEquals("can musicfolder 1", in.readLine());
            assertEquals("can randomplay 0", in.readLine());
            assertEquals("pref httpport 9092", in.readLine());
            assertEquals("pref jivealbumsort artflow", in.readLine());
            assertEquals("version 7.7.2", in.readLine());

            out.println("exit");
            assertEquals("exit", in.readLine());

            assertNull(in.readLine());

            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testAuthentication() {
        SqueezeboxServerMock.starter().username("user").password("1234").start();

        try {
            SocketAddress sa = new InetSocketAddress("localhost", SqueezeboxServerMock.CLI_PORT);
            Socket socket = new Socket();

            socket.connect(sa, 10 * 1000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()),
                    128);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("login user 1234");
            assertEquals("login user ******", in.readLine());

            out.println("exit");
            assertEquals("exit", in.readLine());

            assertNull(in.readLine());

            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testAuthenticationFailure() {
        SqueezeboxServerMock.starter().username("user").password("1234").start();

        try {
            SocketAddress sa = new InetSocketAddress("localhost", SqueezeboxServerMock.CLI_PORT);
            Socket socket = new Socket();

            socket.connect(sa, 10 * 1000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()),
                    128);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("login user wrongpassword");
            assertEquals("login user ******", in.readLine());

            assertNull(in.readLine());

            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testAuthenticatedServer() {
        SqueezeboxServerMock.starter().username("user").password("1234").start();

        try {
            SocketAddress sa = new InetSocketAddress("localhost", SqueezeboxServerMock.CLI_PORT);
            Socket socket = new Socket();

            socket.connect(sa, 10 * 1000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()),
                    128);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("version ?");
            assertNull(in.readLine());

            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
