/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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
import android.util.Log;

import com.google.common.base.Joiner;

import com.crashlytics.android.Crashlytics;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.Playlist;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.model.Year;

class CliClient {

    private static final String TAG = "CliClient";

    /** {@link java.util.regex.Pattern} that splits strings on spaces. */
    private static final Pattern mSpaceSplitPattern = Pattern.compile(" ");

    /**
     * Join multiple strings (skipping nulls) together with newlines.
     */
    private static final Joiner mNewlineJoiner = Joiner.on("\n").skipNulls();

    @IntDef(flag=true, value={
            HANDLER_LIST_GLOBAL, HANDLER_LIST_PREFIXED, HANDLER_LIST_PLAYER_SPECIFIC,
            HANDLER_LIST_GLOBAL_PLAYER_SPECIFIC, HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface HandlerListType {}
    public static final int HANDLER_LIST_GLOBAL = 1;
    public static final int HANDLER_LIST_PREFIXED = 1 << 1;
    public static final int HANDLER_LIST_PLAYER_SPECIFIC = 1 << 2;
    public static final int HANDLER_LIST_GLOBAL_PLAYER_SPECIFIC = 1 << 3;
    public static final int HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC = 1 << 4;

    static class ExtendedQueryFormatCmd {
        private static final int PLAYER_SPECIFIC_HANDLER_LISTS =
                HANDLER_LIST_PLAYER_SPECIFIC | HANDLER_LIST_GLOBAL_PLAYER_SPECIFIC | HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC;

        private static final int PREFIXED_HANDLER_LISTS =
                HANDLER_LIST_PREFIXED | HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC;

        @HandlerListType
        final int handlerList;
        final private boolean playerSpecific;
        final private boolean prefixed;

        final String cmd;

        final private Set<String> taggedParameters;

        final private SqueezeParserInfo[] parserInfos;

        public ExtendedQueryFormatCmd(@HandlerListType int handlerList, String cmd,
                                      Set<String> taggedParameters, SqueezeParserInfo... parserInfos) {
            this.handlerList = handlerList;
            playerSpecific = (PLAYER_SPECIFIC_HANDLER_LISTS & handlerList) != 0;
            prefixed = (PREFIXED_HANDLER_LISTS & handlerList) != 0;
            this.cmd = cmd;
            this.taggedParameters = taggedParameters;
            this.parserInfos = parserInfos;
        }

        /**
         * A command to the server where items in the response have a delimiter other than "id:".
         *
         * @param cmd The command to send to the server.
         * @param taggedParameters The keys for any tagged parameters to send.
         * @param itemDelimiter The identifier of the tagged parameter that marks the start of
         *    a new block of information.
         * @param handler The handler used to construct new model objects from the response.
         */
        public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters,
                                      ListHandler<? extends Item> handler, String... columns) {
            this(HANDLER_LIST_GLOBAL, cmd, taggedParameters, new SqueezeParserInfo(handler, columns));
        }

        public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters,
                                      String itemDelimiter, ListHandler<? extends Item> handler) {
            this(HANDLER_LIST_GLOBAL, cmd, taggedParameters, new SqueezeParserInfo(itemDelimiter, handler));
        }

        /**
         * A command to the server where items in the response are delimited by id: tags.
         *
         * @param cmd The command to send to the server.
         * @param taggedParameters The keys for any tagged parameters to send.
         * @param handler The handler used to construct new model objects from the response.
         */
        public ExtendedQueryFormatCmd(String cmd, Set<String> taggedParameters,
                ListHandler<? extends Item> handler) {
            this(HANDLER_LIST_GLOBAL, cmd, taggedParameters, new SqueezeParserInfo(handler));
        }

        public String toString() {
            return "{ cmd:'" + cmd + "', list:" + handlerList + ", player specific:" + playerSpecific + ", prefixed:" + prefixed + " }";
        }

    }

    final ExtendedQueryFormatCmd[] extQueryFormatCmds = initializeExtQueryFormatCmds();

    final Map<String, ExtendedQueryFormatCmd> extQueryFormatCmdMap
            = initializeExtQueryFormatCmdMap();

    private ExtendedQueryFormatCmd[] initializeExtQueryFormatCmds() {
        List<ExtendedQueryFormatCmd> list = new ArrayList<ExtendedQueryFormatCmd>();

        list.add(
                new ExtendedQueryFormatCmd(
                        "players",
                        new HashSet<String>(Arrays.asList("playerprefs", "charset")),
                        "playerindex",
                        new BaseListHandler<Player>() {}
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "artists",
                        new HashSet<String>(
                                Arrays.asList("search", "genre_id", "album_id", "tags", "charset")),
                        new ArtistListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "albums",
                        new HashSet<String>(
                                Arrays.asList("search", "genre_id", "artist_id", "track_id", "year",
                                        "compilation", "sort", "tags", "charset")),
                        new AlbumListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "years",
                        new HashSet<String>(Arrays.asList("charset")),
                        "year",
                        new BaseListHandler<Year>(){}
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "genres",
                        new HashSet<String>(
                                Arrays.asList("search", "artist_id", "album_id", "track_id", "year",
                                        "tags", "charset")),
                        new GenreListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "musicfolder",
                        new HashSet<String>(Arrays.asList("folder_id", "url", "tags", "charset")),
                        new BaseListHandler<MusicFolderItem>(){}
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "songs",
                        new HashSet<String>(
                                Arrays.asList("genre_id", "artist_id", "album_id", "year", "search",
                                        "tags", "sort", "charset")),
                        new SongListHandler()
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "playlists",
                        new HashSet<String>(Arrays.asList("search", "tags", "charset")),
                        new BaseListHandler<Playlist>(){})
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "playlists tracks",
                        new HashSet<String>(Arrays.asList("playlist_id", "tags", "charset")),
                        "playlist index",
                        new SongListHandler())
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        HANDLER_LIST_GLOBAL,
                        "search",
                        new HashSet<String>(Arrays.asList("term", "charset")),
                        new SqueezeParserInfo("genres_count", new GenreListHandler(), "genre_id"),
                        new SqueezeParserInfo("albums_count", new AlbumListHandler(), "album_id"),
                        new SqueezeParserInfo("contributors_count", new ArtistListHandler()
                                , "contributor_id"),
                        new SqueezeParserInfo("tracks_count", new SongListHandler(), "track_id")
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        HANDLER_LIST_PLAYER_SPECIFIC,
                        "status",
                        new HashSet<String>(Arrays.asList("tags", "charset", "subscribe")),
                        new SqueezeParserInfo("playlist_tracks", new SongListHandler(),
                                "playlist index")
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "radios",
                        new HashSet<String>(Arrays.asList("sort", "charset")),
                        new PluginListHandler(),
                        "cmd", "name", "type", "icon", "weight"
                )

        );
        list.add(
                new ExtendedQueryFormatCmd(
                        "apps",
                        new HashSet<String>(Arrays.asList("sort", "charset")),
                        new PluginListHandler(),
                        "cmd", "name", "type", "icon", "weight"
                )
        );
        list.add(
                new ExtendedQueryFormatCmd(
                        HANDLER_LIST_PREFIXED_PLAYER_SPECIFIC,
                        "items",
                        new HashSet<String>(
                                Arrays.asList("item_id", "search", "want_url", "charset")),
                        new SqueezeParserInfo(new BaseListHandler<PluginItem>(){}))
        );

        return list.toArray(new ExtendedQueryFormatCmd[list.size()]);
    }

    private Map<String, ExtendedQueryFormatCmd> initializeExtQueryFormatCmdMap() {
        Map<String, ExtendedQueryFormatCmd> map = new HashMap<String, ExtendedQueryFormatCmd>();
        for (ExtendedQueryFormatCmd cmd : extQueryFormatCmds) {
            map.put(cmd.cmd, cmd);
        }
        return map;
    }

    private final SqueezeService service;

    private int pageSize;

    CliClient(SqueezeService service) {
        this.service = service;
    }

    void initialize() {
        pageSize = service.getResources().getInteger(R.integer.PageSize);
    }


    // All requests are tagged with a correlation id, which can be used when
    // asynchronous responses are received.
    private int _correlationid = 0;


    /**
     * Send the supplied commands to the SqueezeboxServer.
     * <p/>
     * <b>All</b> data to the server goes through this method
     * <p/>
     * <b>Note</b> don't call this from the main (UI) thread. If you are unsure if you are on the
     * main thread, then use {@link #sendCommand(String...)} instead.
     *
     * @param commands List of commands to send
     */
    synchronized void sendCommandImmediately(String... commands) {
        if (commands.length == 0) {
            return;
        }
        PrintWriter writer = service.connectionState.getSocketWriter();
        if (writer == null) {
            return;
        }

        String formattedCommands = mNewlineJoiner.join(commands);
        Log.v(TAG, "SEND: " + formattedCommands);

        // Make sure that username/password do not make it to Crashlytics.
        if (commands[0].startsWith("login ")) {
            Crashlytics.setString("lastCommands", "login [username] [password]");
        } else {
            Crashlytics.setString("lastCommands", formattedCommands);
        }

        writer.println(formattedCommands);
        writer.flush();
    }

    /**
     * Send the supplied commands to the SqueezeboxServer.
     * <p/>
     * This method takes care to avoid performing network operations on the main thread. Use {@link
     * #sendCommandImmediately(String...)} if you are sure you are not on the main thread (eg if
     * called from the listening thread).
     *
     * @param commands List of commands to send
     */
    void sendCommand(final String... commands) {
        if (service.mainThread != Thread.currentThread()) {
            sendCommandImmediately(commands);
        } else {
            service.executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendCommandImmediately(commands);
                }
            });
        }
    }

    /**
     * Send the specified command for the specified player to the SqueezeboxServer
     *
     * @param command The command to send
     */
    void sendPlayerCommand(final Player player, final String command) {
        sendCommand(Util.encode(player.getId()) + " " + command);
    }

    /**
     * Send the specified command for the active player to the SqueezeboxServer
     *
     * @param command The command to send
     */
    void sendActivePlayerCommand(final String command) {
        if (service.connectionState.getActivePlayer() == null) {
            return;
        }
        sendPlayerCommand(service.connectionState.getActivePlayer(), command);
    }


    /**
     * Keeps track of asynchronous request waiting for a reply
     * <p>
     * When a request is made, the callback is put this list, along with a
     * unique correlation id.
     * <p>
     * When the reply comes the callback is called, and the request is removed from this list.
     * <p>
     * When the client hosting callbacks goes away, all requests with callbacks hosted by it, is
     * removed from this list.
     * <p>
     * If a reply with with matching entry is this list comes in, it is discarded.
     */
    private final Map<Integer, IServiceItemListCallback> pendingRequests
            = new ConcurrentHashMap<Integer, IServiceItemListCallback>();

    public void cancelClientRequests(Object client) {
        for (Map.Entry<Integer, IServiceItemListCallback> entry : pendingRequests.entrySet()) {
            if (entry.getValue().getClient() == client) {
                Log.i(TAG, "cancel request: [" + entry.getKey() + ";" + entry.getValue() +"]");
                pendingRequests.remove(entry.getKey());
            }
        }
    }

    /**
     * Send an asynchronous request to the SqueezeboxServer for the specified items.
     * <p/>
     * Items are requested in chunks of <code>R.integer.PageSize</code>, and returned
     * to the caller via the specified callback.
     * <p/>
     * If start is zero, this will order one item, to quickly learn the number of items
     * from the server. When the server response with this item it is transferred to the
     * caller. The remaining items in the first page are then ordered, and transferred
     * to the caller when they arrive.
     * <p/>
     * If start is < 0, it means the caller wants the entire list. They are ordered in
     * pages, and transferred to the caller as they arrive.
     * <p/>
     * Otherwise request a page of items starting from start.
     * <p/>
     * See {@link #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)} for details.
     *
     * @param playerId Id of the current player or null
     * @param cmd Identifies the type of items
     * @param start First item to return
     * @param parameters Item specific parameters for the request
     * @see #parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)
     */
    private void requestItems(String playerId, String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        boolean full_list = (start < 0);

        pendingRequests.put(_correlationid, callback);
        final StringBuilder sb = new StringBuilder(
                cmd + " " + (full_list ? 0 : start) + " " + (start == 0 ? 1 : pageSize));
        if (playerId != null) {
            sb.insert(0, Util.encode(playerId) + " ");
        }
        if (parameters != null) {
            for (String parameter : parameters) {
                sb.append(" ").append(Util.encode(parameter));
            }
        }
        if (full_list)
            sb.append(" full_list:1");
        sb.append(" correlationid:");
        sb.append(_correlationid++);
        sendCommand(sb.toString());
    }

    void requestItems(String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        requestItems(null, cmd, start, parameters, callback);
    }

    void requestItems(String cmd, int start, IServiceItemListCallback callback) {
        requestItems(cmd, start, null, callback);
    }

    void requestPlayerItems(String cmd, int start, List<String> parameters, IServiceItemListCallback callback) {
        if (service.connectionState.getActivePlayer() == null) {
            return;
        }
        requestItems(service.connectionState.getActivePlayer().getId(), cmd, start, parameters, callback);
    }

    void requestPlayerItems(String cmd, int start, IServiceItemListCallback callback) {
        requestPlayerItems(cmd, start, null, callback);
    }

    /**
     * Data for {@link CliClient#parseSqueezerList(CliClient.ExtendedQueryFormatCmd, List)}
     *
     * @author kaa
     */
    private static class SqueezeParserInfo {

        private final Set<String> columns;

        private final String count_id;

        private final ListHandler<? extends Item> handler;

        /**
         * @param countId The label for the tag which contains the total number of results, normally
         * "count".
         * @param handler Callback to receive the parsed data.
         * @param columns If one column is specified, it is the item delimiter as defined for each
         *                extended query format command in the SqueezeServer CLI documentation.
         *                Multiple columns is supported to workaround of a bug in recent server
         *                versions.
         */
        public SqueezeParserInfo(String countId, ListHandler<? extends Item> handler, String... columns) {
            count_id = countId;
            this.columns = new HashSet<String>(Arrays.asList(columns));
            this.handler = handler;
        }

        public SqueezeParserInfo(String itemDelimiter, ListHandler<? extends Item> handler) {
            this("count", handler, itemDelimiter);
        }

        public SqueezeParserInfo(ListHandler<? extends Item> handler, String... columns) {
            this("count", handler, columns);
        }

        public SqueezeParserInfo(ListHandler<? extends Item> handler) {
            this("id", handler);
        }

        public boolean isComplete(Map<String, String> record) {
            for (String column : columns) {
                if (!record.containsKey(column)) return false;
            }
            return true;
        }
    }

    /**
     * Generic method to parse replies for queries in extended query format
     * <p/>
     * This is the control center for asynchronous and paging receiving of data from SqueezeServer.
     * <p/>
     * Transfer of each data type are started by an asynchronous request by one of the public method
     * in this module. This method will forward the data using the supplied {@link ListHandler}, and
     * and order the next page if necessary, repeating the current query parameters.
     * <p/>
     * Activities should just initiate the request, and supply a callback to receive a page of
     * data.
     *
     * @param cmd Describes of the CLI command
     * @param tokens List of tokens with value or key:value.
     */
    void parseSqueezerList(ExtendedQueryFormatCmd cmd, List<String> tokens) {
        Log.v(TAG, "Parsing list, cmd: " +cmd + ", tokens: " + tokens);

        final int ofs = mSpaceSplitPattern.split(cmd.cmd).length + (cmd.playerSpecific ? 1 : 0) + (cmd.prefixed ? 1 : 0);
        int actionsCount = 0;
        final String playerid = (cmd.playerSpecific ? tokens.get(0) + " " : "");
        final String prefix = (cmd.prefixed ? tokens.get(cmd.playerSpecific ? 1 : 0) + " " : "");
        final int start = Util.parseDecimalIntOrZero(tokens.get(ofs));
        final int itemsPerResponse = Util.parseDecimalIntOrZero(tokens.get(ofs + 1));

        int correlationId = 0;
        boolean rescan = false;
        boolean full_list = false;
        final Map<String, String> taggedParameters = new HashMap<String, String>();
        final Map<String, String> parameters = new HashMap<String, String>();
        final Set<String> countIdSet = new HashSet<String>();
        final Map<String, SqueezeParserInfo> itemDelimeterMap = new HashMap<String, SqueezeParserInfo>();
        final Map<String, Integer> counts = new HashMap<String, Integer>();
        final Map<String, String> record = new HashMap<String, String>();

        for (SqueezeParserInfo parserInfo : cmd.parserInfos) {
            parserInfo.handler.clear();
            countIdSet.add(parserInfo.count_id);
            for (String column : parserInfo.columns) itemDelimeterMap.put(column, parserInfo);
        }

        SqueezeParserInfo parserInfo = null;
        for (int idx = ofs + 2; idx < tokens.size(); idx++) {
            String token = tokens.get(idx);
            int colonPos = token.indexOf("%3A");
            if (colonPos == -1) {
                Log.e(TAG, "Expected colon in list token. '" + token + "'");
                return;
            }
            String key = Util.decode(token.substring(0, colonPos));
            String value = Util.decode(token.substring(colonPos + 3));
            Log.v(TAG, "key=" + key + ", value: " + value);

            if ("rescan".equals(key)) {
                rescan = (Util.parseDecimalIntOrZero(value) == 1);
            } else if ("full_list".equals(key)) {
                full_list = (Util.parseDecimalIntOrZero(value) == 1);
                taggedParameters.put(key, token);
            } else if ("correlationid".equals(key)) {
                correlationId = Util.parseDecimalIntOrZero(value);
                taggedParameters.put(key, token);
            } else if ("actions".equals(key)) {
                // Apparently squeezer returns some commands which are
                // included in the count of the current request
                actionsCount++;
            }
            if (countIdSet.contains(key)) {
                counts.put(key, Util.parseDecimalIntOrZero(value));
            } else {
                SqueezeParserInfo newParserInfo = itemDelimeterMap.get(key);
                if (newParserInfo != null && parserInfo != null && parserInfo.isComplete(record)) {
                    parserInfo.handler.add(record);
                    Log.v(TAG, "record=" + record);
                    record.clear();
                }
                if (newParserInfo != null) parserInfo = newParserInfo;
                if (parserInfo != null) {
                    record.put(key, value);
                } else if (cmd.taggedParameters.contains(key)) {
                    taggedParameters.put(key, token);
                } else {
                    parameters.put(key, value);
                }
            }
        }

        if (parserInfo != null && !record.isEmpty()) {
            parserInfo.handler.add(record);
            Log.v(TAG, "record=" + record);
        }

        // Process the lists for all the registered handlers
        int end = start + itemsPerResponse;
        int max = 0;
        IServiceItemListCallback callback = pendingRequests.get(correlationId);
        for (SqueezeParserInfo parser : cmd.parserInfos) {
            Integer count = counts.get(parser.count_id);
            int countValue = (count == null ? 0 : count);
            if (count != null || start == 0) {
                if (callback != null) {
                    callback.onItemsReceived(countValue - actionsCount, start, parameters, parser.handler.getItems(), parser.handler.getDataType());
                }
                if (countValue > max) {
                    max = countValue;
                }
            }
        }

        // If the client is still around check if we need to order more items,
        // otherwise were done, so remove the callback
        if (callback != null) {
            if ((full_list || end % pageSize != 0) && end < max) {
                int count = (end + pageSize > max ? max - end : full_list ? pageSize : pageSize - itemsPerResponse);
                StringBuilder cmdline = new StringBuilder();
                cmdline.append(playerid);
                cmdline.append(prefix);
                cmdline.append(cmd.cmd);
                cmdline.append(" ");
                cmdline.append(end);
                cmdline.append(" ");
                cmdline.append(count);
                for (String parameter : taggedParameters.values()) {
                    cmdline.append(" ").append(parameter);
                }
                sendCommandImmediately(cmdline.toString());
            } else
                pendingRequests.remove(correlationId);
        }
    }

    private class GenreListHandler extends BaseListHandler<Genre> {}

    private class ArtistListHandler extends BaseListHandler<Artist> {}

    private class AlbumListHandler extends BaseListHandler<Album> {}

    private class SongListHandler extends BaseListHandler<Song> {}

    private class PluginListHandler extends BaseListHandler<Plugin> {}

}
