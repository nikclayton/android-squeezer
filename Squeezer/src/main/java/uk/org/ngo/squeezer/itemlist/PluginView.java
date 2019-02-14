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

package uk.org.ngo.squeezer.itemlist;

import android.app.Activity;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class PluginView extends BaseItemView<Plugin> implements IServiceItemListCallback<Plugin> {

    private final BaseListActivity<Plugin> activity;

    public PluginView(BaseListActivity<Plugin> activity) {
        super(activity);
        this.activity = activity;

        setLoadingViewParams(VIEW_PARAM_ICON);
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, int position, Plugin item) {
        @ViewParam int viewParams = (VIEW_PARAM_ICON | (item.hasContextMenu() ? VIEW_PARAM_CONTEXT_BUTTON : 0));
        View view = getAdapterView(convertView, parent, viewParams);
        bindView(view, item);
        return view;
    }

    @Override
    public void bindView(View view, Plugin item) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());
        // If the item has an image, then fetch and display it
        if (item.getIcon() != null) {
            ImageFetcher.getInstance(getActivity()).loadImage(item.getIcon(), viewHolder.icon,
                    mIconWidth, mIconHeight);
        } else {
            viewHolder.icon.setImageResource(item.getIconResource());
        }

    }

    @Override
    public String getQuantityString(int quantity) {
        throw new UnsupportedOperationException("quantities are not supported for plugins");
    }

    @Override
    public boolean isSelectable(Plugin item) {
        return item.isSelectable();
    }

    @Override
    public void onItemSelected(int index, Plugin item) {
        Action.NextWindow nextWindow = (item.goAction != null ? item.goAction.action.nextWindow : item.nextWindow);
        if (nextWindow == null) {
            if (item.goAction != null)
                PluginListActivity.show(getActivity(), item, item.goAction);
            else if (item.hasSubItems())
                PluginListActivity.show(getActivity(), item);
            else if (item.getNode() != null)
                HomeMenuActivity.show(getActivity(), item.getId());
        } else {
            getActivity().action(item, item.goAction);
            switch (nextWindow.nextWindow) {
                case nowPlaying:
                    // Do nothing as now playing is always available in Squeezer (maybe toast the action)
                    break;
                case playlist:
                    CurrentPlaylistActivity.show(getActivity());
                    break;
                case home:
                    HomeActivity.show(getActivity());
                    break;
                case parent:
                case parentNoRefresh:
                    getActivity().finish();
                    break;
                case grandparent:
                    getActivity().setResult(Activity.RESULT_OK, new Intent(PluginListActivity.FINISH));
                    getActivity().finish();
                    break;
                case refresh:
                    getActivity().clearAndReOrderItems();
                    break;
                case refreshOrigin:
                    getActivity().setResult(Activity.RESULT_OK, new Intent(PluginListActivity.RELOAD));
                    getActivity().finish();
                    break;
                case windowId:
                    //TODO implement
                    break;
            }
        }
   }

    // Only touch these from the main thread
    private boolean contextMenuReady = false;
    private boolean contextMenuWaiting = false;
    private Stack<Action> contextStack;
    private View contextMenuView;
    private Plugin contextItem;
    private String contextMenuTitle;
    private List<Plugin> contextItems;

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, ItemView.ContextMenuInfo menuInfo) {
        final Plugin item = (Plugin) menuInfo.item;
        if (!contextMenuReady && !contextMenuWaiting) {
            contextMenuTitle = null;
            contextItem = null;
            contextItems = null;
            if (item.hasSlimContextMenu()) {
                contextMenuView = v;
                contextItem = item;
                contextStack = new Stack<>();
                contextStack.push(item.moreAction);
                orderContextMenu(item.moreAction);
            } else {
                if (item.playAction() != null) {
                    menu.add(Menu.NONE, R.id.play_now, Menu.NONE, R.string.PLAY_NOW);
                }
                if (item.addAction != null) {
                    menu.add(Menu.NONE, R.id.add_to_playlist, Menu.NONE, R.string.ADD_TO_END);
                }
                if (item.insertAction != null) {
                    menu.add(Menu.NONE, R.id.play_next, Menu.NONE, R.string.PLAY_NEXT);
                }
                if (item.moreAction != null) {
                    menu.add(Menu.NONE, R.id.more, Menu.NONE, getActivity().getServerString(ServerString.MORE));
                }
            }
        } else if (contextMenuReady) {
            contextMenuReady = false;
            ViewHolder viewHolder = (ViewHolder) contextMenuView.getTag();
            viewHolder.contextMenuButton.setVisibility(View.VISIBLE);
            viewHolder.contextMenuLoading.setVisibility(View.INVISIBLE);
            if (contextStack.size() > 1) {
                View headerVew = getLayoutInflater().inflate(R.layout.context_menu_header, (ViewGroup) v, false);
                menu.setHeaderView(headerVew);
                ImageView backButton = (ImageView) headerVew.findViewById(R.id.back);
                if (contextStack.size() > 1) {
                    backButton.setVisibility(View.VISIBLE);
                    backButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            menu.close();
                            contextStack.pop();
                            orderContextMenu(contextStack.peek());
                        }
                    });
                } else {
                    backButton.setVisibility(View.GONE);
                }
                if (contextMenuTitle != null) {
                    ((TextView) headerVew.findViewById(R.id.title)).setText(contextMenuTitle);
                }
            } else if (contextMenuTitle != null) {
                menu.setHeaderTitle(contextMenuTitle);
            }
            int index = 0;
            for (Plugin plugin : contextItems) {
                menu.add(Menu.NONE, index++, Menu.NONE, plugin.getName()).setEnabled(plugin.goAction != null);
            }
        }
    }

    private void orderContextMenu(Action action) {
        ISqueezeService service = activity.getService();
        if (service != null) {
            contextMenuWaiting = true;
            ViewHolder viewHolder = (ViewHolder) contextMenuView.getTag();
            viewHolder.contextMenuButton.setVisibility(View.INVISIBLE);
            viewHolder.contextMenuLoading.setVisibility(View.VISIBLE);
            service.pluginItems(action, this);
        }
    }

    @Override
    public boolean doItemContext(MenuItem menuItem, int index, Plugin selectedItem) {
        if (contextItems != null) {
            selectedItem = contextItems.get(menuItem.getItemId());
            if (selectedItem.goAction.action.nextWindow != null) {
                getActivity().action(contextMenuTitle, contextItem, selectedItem.goAction);
                switch (selectedItem.goAction.action.nextWindow.nextWindow) {
                    case playlist:
                        CurrentPlaylistActivity.show(getActivity());
                        break;
                    case home:
                        HomeActivity.show(getActivity());
                        break;
                    case parent: // For centext menus parent and grandparent hide the context menu(s) and reload items
                    case grandparent:
                    case parentNoRefresh:
                    case refresh:
                    case refreshOrigin:
                        break;
                    case windowId:
                        //TODO implement
                        break;
                }
            } else {
                if (selectedItem.goAction.isContextMenu()) {
                    contextStack.push(selectedItem.goAction);
                    orderContextMenu(selectedItem.goAction);
                } else {
                    PluginListActivity.show(getActivity(), contextItem, selectedItem.goAction);
                }
            }
            return true;
        } else {
        switch (menuItem.getItemId()) {
            case R.id.play_now:
                getActivity().action(selectedItem, selectedItem.playAction());
                return true;
            case R.id.add_to_playlist:
                getActivity().action(selectedItem, selectedItem.addAction);
                return true;
            case R.id.play_next:
                getActivity().action(selectedItem, selectedItem.insertAction);
                return true;
            case R.id.more:
                PluginListActivity.show(getActivity(), selectedItem, selectedItem.moreAction);
                return true;
        }
        return false;
    }
    }

    @Override
    public Object getClient() {
        return getActivity();
    }

    @Override
    public void onItemsReceived(int count, int start, final Map<String, Object> parameters, final List<Plugin> items, Class<Plugin> dataType) {
        getActivity().getUIThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                contextMenuReady = true;
                contextMenuWaiting = false;
                if (parameters.containsKey("title")) {
                    contextMenuTitle = Util.getString(parameters, "title");
                }
                contextItems = items;
                activity.getListView().showContextMenu();
            }
        });
    }
}
