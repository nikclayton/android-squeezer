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
package uk.org.ngo.squeezer.itemlist.dialog;

import android.content.Context;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.EnumWithTextAndIcon;
import uk.org.ngo.squeezer.framework.VersionedEnumWithText;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.service.ServerVersion;

public class AlbumViewDialog extends BaseViewDialog<Album, AlbumViewDialog.AlbumListLayout, AlbumViewDialog.AlbumsSortOrder> {

    @Override
    protected String getTitle() {
        return ServerString.ALBUM_DISPLAY_OPTIONS.getLocalizedString();
    }

    /**
     * Supported album list layouts.
     */
    public enum AlbumListLayout implements EnumWithTextAndIcon {
        grid(R.attr.ic_action_view_as_grid, ServerString.SWITCH_TO_GALLERY),
        list(R.attr.ic_action_view_as_list, ServerString.SWITCH_TO_EXTENDED_LIST);

        /**
         * The icon to use for this layout
         */
        private final int iconAttribute;

        @Override
        public int getIconAttribute() {
            return iconAttribute;
        }

        /**
         * The text to use for this layout
         */
        private final ServerString serverString;

        @Override
        public String getText(Context context) {
            return serverString.getLocalizedString();
        }

        AlbumListLayout(int iconAttribute, ServerString serverString) {
            this.serverString = serverString;
            this.iconAttribute = iconAttribute;
        }
    }

    /**
     * Sort order strings supported by the server.
     * <p>
     * Values must correspond with the string expected by the server. Any '__' in the strings will
     * be removed.
     */
    public enum AlbumsSortOrder implements VersionedEnumWithText {
        __new(ServerString.BROWSE_NEW_MUSIC),
        album(ServerString.ALBUM),
        artflow(ServerString.SORT_ARTISTYEARALBUM),
        artistalbum(ServerString.SORT_ARTISTALBUM),
        yearalbum(ServerString.SORT_YEARALBUM),
        yearartistalbum(ServerString.SORT_YEARARTISTALBUM);

        private final ServerString serverString;

        @Override
        public boolean can(ServerVersion version) {
            return true;
        }

        @Override
        public String getText(Context context) {
            return serverString.getLocalizedString();
        }

        AlbumsSortOrder(ServerString serverString) {
            this.serverString = serverString;
        }
    }
}
