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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.NowPlayingActivity;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.dialog.NetworkErrorDialogFragment;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.ViewParamItemView;
import uk.org.ngo.squeezer.model.Action;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.model.Window;
import uk.org.ngo.squeezer.itemlist.dialog.ArtworkListLayout;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.util.ImageFetcher;

import static com.google.common.base.Preconditions.checkNotNull;

/*
 * The activity's content view scrolls in from the right, and disappear to the left, to provide a
 * spatial component to navigation.
 */
public class JiveItemListActivity extends BaseListActivity<JiveItemView, JiveItem>
        implements NetworkErrorDialogFragment.NetworkErrorDialogListener {
    private static final int GO = 1;
    private static final String FINISH = "FINISH";
    private static final String RELOAD = "RELOAD";

    private JiveItemViewLogic pluginViewDelegate;
    private boolean register;
    protected JiveItem parent;
    private Action action;
    Window window = new Window();
    private int selectedIndex;

    private MenuItem menuItemList;
    private MenuItem menuItemGrid;
    private ViewParamItemView<JiveItem> parentViewHolder;

    @Override
    protected ItemAdapter<JiveItemView, JiveItem> createItemListAdapter() {
        return new ItemAdapter<JiveItemView, JiveItem>(this) {

            @Override
            public JiveItemView createViewHolder(View view) {
                return new JiveItemView(JiveItemListActivity.this, view);
            }

            @Override
            protected int getItemViewType(JiveItem item) {
                return item != null && item.hasSlider() ?
                        R.layout.slider_item :
                        (JiveItemView.listLayout(JiveItemListActivity.this, window.windowStyle) == ArtworkListLayout.grid) ? R.layout.grid_item : R.layout.list_item;
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = checkNotNull(getIntent().getExtras(), "intent did not contain extras");
        register = extras.getBoolean("register");
        parent = extras.getParcelable(JiveItem.class.getName());
        action = extras.getParcelable(Action.class.getName());

        pluginViewDelegate = new JiveItemViewLogic(this);
        setParentViewHolder();

        // If initial setup is performed, use it
        if (savedInstanceState != null && savedInstanceState.containsKey("window")) {
            applyWindow((Window) savedInstanceState.getParcelable("window"));
        } else {
            if (parent != null && parent.window != null) {
                applyWindow(parent.window);
            } else if (parent != null && "playlist".equals(parent.getType())) {
                // special case of playlist - override server based windowStyle to play_list
                applyWindowStyle(Window.WindowStyle.PLAY_LIST);
            } else
                applyWindowStyle(Window.WindowStyle.TEXT_ONLY);
        }

        findViewById(R.id.input_view).setVisibility((hasInputField()) ? View.VISIBLE : View.GONE);
        if (hasInputField()) {
            MaterialButton inputButton = findViewById(R.id.input_button);
            final EditText inputText = findViewById(R.id.plugin_input);
            TextInputLayout inputTextLayout = findViewById(R.id.plugin_input_til);
            int inputType = EditorInfo.TYPE_CLASS_TEXT;
            int inputImage = R.drawable.keyboard_return;

            switch (action.getInputType()) {
                case TEXT:
                    break;
                case SEARCH:
                    inputImage = R.drawable.ic_menu_search;
                    break;
                case EMAIL:
                    inputType |= EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
                    break;
                case PASSWORD:
                    inputType |= EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;
                    break;
            }
            inputText.setInputType(inputType);
            inputButton.setIconResource(inputImage);
            inputTextLayout.setHint(parent.input.title);
            inputText.setText(parent.input.initialText);
            inputText.setOnKeyListener((v, keyCode, event) -> {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    clearAndReOrderItems(inputText.getText().toString());
                    return true;
                }
                return false;
            });

            inputButton.setOnClickListener(v -> {
                if (getService() != null) {
                    clearAndReOrderItems(inputText.getText().toString());
                }
            });
        }
    }

    private void setParentViewHolder() {
        parentViewHolder = new ViewParamItemView<>(this, findViewById(R.id.parent_container));
        parentViewHolder.contextMenuButton.setOnClickListener(v -> pluginViewDelegate.showContextMenu(parentViewHolder, parent));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("window", window);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupListView();
    }

    @Override
    public void onPause() {
        super.onPause();
        pluginViewDelegate.resetContextMenu();
        pluginViewDelegate.resetContextMenu();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        getListView().setRecyclerListener(viewHolder -> {
            // Release strong reference when a view is recycled
            final ImageView imageView = ((JiveItemView)viewHolder).icon;
            if (imageView != null) {
                imageView.setImageBitmap(null);
            }
        });

        setupListView();
    }

    private void setupListView() {
        ArtworkListLayout listLayout = JiveItemView.listLayout(this, window.windowStyle);
        RecyclerView.LayoutManager layoutManager = getListView().getLayoutManager();
        if (listLayout == ArtworkListLayout.grid && !(layoutManager instanceof GridLayoutManager)) {
            // TODO calculate column count OR use a (vertical) LinearLayoutManager for rows with a horizontal LinearLayout on each row
            getListView().setLayoutManager(new GridLayoutManager(this, 2));
            getListView().removeItemDecorationAt(0);
        }
        if (listLayout == ArtworkListLayout.list && (layoutManager instanceof GridLayoutManager)) {
            getListView().setLayoutManager(new LinearLayoutManager(this));
            getListView().addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        }
    }

    void updateHeader(String windowTitle) {
        window.text = windowTitle;

        parentViewHolder.itemView.setVisibility(View.VISIBLE);
        parentViewHolder.text1.setText(windowTitle);
        parentViewHolder.icon.setVisibility(View.GONE);
        parentViewHolder.contextMenuButtonHolder.setVisibility(View.GONE);
    }

    void updateHeader(JiveItem parent) {
        updateHeader(parent.getName());

        if (parent.hasArtwork() && window.windowStyle == Window.WindowStyle.TEXT_ONLY) {
            parentViewHolder.text2.setVisibility(View.VISIBLE);
            parentViewHolder.text2.setText(parent.text2);

            parentViewHolder.icon.setVisibility(View.VISIBLE);
            ImageFetcher.getInstance(this).loadImage(parent.getIcon(), parentViewHolder.icon);
        }
        if (parent.hasContextMenu()) {
            parentViewHolder.contextMenuButtonHolder.setVisibility(View.VISIBLE);
        }

    }

    private void updateHeader(@NonNull Window window) {
        if (!TextUtils.isEmpty(window.text)) {
            updateHeader(window.text);
        }
        if (!TextUtils.isEmpty(window.textarea)) {
            TextView header = findViewById(R.id.sub_header);
            header.setText(window.textarea);
            findViewById(R.id.sub_header_container).setVisibility(View.VISIBLE);
        }
    }

    private void applyWindow(@NonNull Window window) {
        applyWindowStyle(register ? Window.WindowStyle.TEXT_ONLY : window.windowStyle);
        updateHeader(window);

        window.titleStyle = this.window.titleStyle;
        window.text = this.window.text;
        this.window = window;
    }


    void applyWindowStyle(Window.WindowStyle windowStyle) {
        applyWindowStyle(windowStyle, JiveItemView.listLayout(this, window.windowStyle));
    }

    void applyWindowStyle(Window.WindowStyle windowStyle, ArtworkListLayout prevListLayout) {
        ArtworkListLayout listLayout = JiveItemView.listLayout(this, windowStyle);
        updateViewMenuItems(listLayout, windowStyle);
        if (windowStyle != window.windowStyle || listLayout != prevListLayout) {
            window.windowStyle = windowStyle;
            getItemAdapter().notifyDataSetChanged();
        }
        if (listLayout != prevListLayout) {
            setupListView();
        }
    }


    private void clearAndReOrderItems(String inputString) {
        if (getService() != null && !TextUtils.isEmpty(inputString)) {
            parent.inputValue = inputString;
            clearAndReOrderItems();
        }
    }

    private boolean hasInputField() {
        return parent != null && parent.hasInputField();
    }

    @Override
    protected boolean needPlayer() {
        // Most of the the times we actually do need a player, but if we need to register on SN,
        // it is before we can get the players
        return !register;
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        if (parent != null) {
            if (action == null || (parent.hasInput() && !parent.isInputReady())) {
                showContent();
            } else
                service.pluginItems(start, parent, action, this);
        } else if (register) {
            service.register(this);
        }
    }

    public void onEventMainThread(HandshakeComplete event) {
        super.onEventMainThread(event);
        if (parent != null && parent.hasSubItems()) {
            getItemAdapter().update(parent.subItems.size(), 0, parent.subItems);
        }
    }

    @Override
    public void onItemsReceived(int count, int start, final Map<String, Object> parameters, List<JiveItem> items, Class<JiveItem> dataType) {
        if (parameters.containsKey("goNow")) {
            Action.NextWindow nextWindow = Action.NextWindow.fromString(Util.getString(parameters, "goNow"));
            switch (nextWindow.nextWindow) {
                case nowPlaying:
                    NowPlayingActivity.show(this);
                    break;
                case playlist:
                    CurrentPlaylistActivity.show(this);
                    break;
                case home:
                    HomeActivity.show(this);
                    break;
            }
            finish();
            return;
        }

        final Window window = JiveItem.extractWindow(Util.getRecord(parameters, "window"), null);
        if (window != null) {
            // override server based icon_list style for playlist
            if (window.windowStyle == Window.WindowStyle.ICON_LIST && parent != null && "playlist".equals(parent.getType())) {
                window.windowStyle = Window.WindowStyle.PLAY_LIST;
            }
            runOnUiThread(() -> applyWindow(window));
        }

        if (this.window.text == null && parent != null) {
            runOnUiThread(() -> updateHeader(parent));
        }

        // The documentation says "Returned with value 1 if there was a network error accessing
        // the content source.". In practice (with at least the Napster and Pandora plugins) the
        // value is an error message suitable for displaying to the user.
        if (parameters.containsKey("networkerror")) {
            Resources resources = getResources();
            ISqueezeService service = getService();
            String playerName;

            if (service == null) {
                playerName = "Unknown";
            } else {
                playerName = service.getActivePlayer().getName();
            }

            String errorMsg = Util.getString(parameters, "networkerror");

            String errorMessage = String.format(resources.getString(R.string.server_error),
                    playerName, errorMsg);
            NetworkErrorDialogFragment networkErrorDialogFragment =
                    NetworkErrorDialogFragment.newInstance(errorMessage);
            networkErrorDialogFragment.show(getSupportFragmentManager(), "networkerror");
        }

        super.onItemsReceived(count, start, parameters, items, dataType);
    }

    @Override
    public void action(JiveItem item, Action action, int alreadyPopped) {
        if (getService() == null) {
            return;
        }

        if (action != null) {
            getService().action(item, action);
        }

        Action.JsonAction jAction = (action != null && action.action != null) ? action.action : null;
        Action.NextWindow nextWindow = (jAction != null ? jAction.nextWindow : item.nextWindow);
        nextWindow(nextWindow, alreadyPopped);
    }

    @Override
    public void action(Action.JsonAction action, int alreadyPopped) {
        if (getService() == null) {
            return;
        }

        getService().action(action);
        nextWindow(action.nextWindow, alreadyPopped);
    }

    private void nextWindow(Action.NextWindow nextWindow, int alreadyPopped) {
        while (alreadyPopped > 0 && nextWindow != null) {
            nextWindow = popNextWindow(nextWindow);
            alreadyPopped--;
        }
        if (nextWindow != null) {
            switch (nextWindow.nextWindow) {
                case nowPlaying:
                    // Do nothing as now playing is always available in Squeezer (maybe toast the action)
                    break;
                case playlist:
                    CurrentPlaylistActivity.show(this);
                    break;
                case home:
                    HomeActivity.show(this);
                    break;
                case parentNoRefresh:
                    finish();
                    break;
                case grandparent:
                    setResult(Activity.RESULT_OK, new Intent(FINISH));
                    finish();
                    break;
                case refresh:
                    clearAndReOrderItems();
                    break;
                case parent:
                case refreshOrigin:
                    setResult(Activity.RESULT_OK, new Intent(RELOAD));
                    finish();
                    break;
                case windowId:
                    //TODO implement
                    break;
            }
        }
    }

    private Action.NextWindow popNextWindow(Action.NextWindow nextWindow) {
        switch (nextWindow.nextWindow) {
            case parent:
            case parentNoRefresh:
                return null;
            case grandparent:
                return new Action.NextWindow(Action.NextWindowEnum.parentNoRefresh);
            case refreshOrigin:
                return new Action.NextWindow(Action.NextWindowEnum.refresh);
            default:
                return nextWindow;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GO) {
            if (resultCode == RESULT_OK) {
                if (FINISH.equals(data.getAction())) {
                    finish();
                } else if (RELOAD.equals(data.getAction())) {
                    clearAndReOrderItems();
                }
            }
        }
    }

    public void setPreferredListLayout(ArtworkListLayout listLayout) {
        ArtworkListLayout prevListLayout = JiveItemView.listLayout(this, window.windowStyle);
        saveListLayout(listLayout);
        applyWindowStyle(window.windowStyle, prevListLayout);
    }

    protected void saveListLayout(ArtworkListLayout listLayout) {
        new Preferences(this).setAlbumListLayout(listLayout);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
    }

    /**
     * The user dismissed the network error dialog box. There's nothing more to do, so finish
     * the activity.
     */
    @Override
    public void onDialogDismissed(DialogInterface dialog) {
        runOnUiThread(this::finish);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pluginlistmenu, menu);
        menuItemList = menu.findItem(R.id.menu_item_list);
        menuItemGrid = menu.findItem(R.id.menu_item_grid);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateViewMenuItems(getPreferredListLayout(), window.windowStyle);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_list:
                setPreferredListLayout(ArtworkListLayout.list);
                return true;
            case R.id.menu_item_grid:
                setPreferredListLayout(ArtworkListLayout.grid);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateViewMenuItems(ArtworkListLayout listLayout, Window.WindowStyle windowStyle) {
        boolean canChangeListLayout = JiveItemView.canChangeListLayout(windowStyle);
        if (menuItemList != null) {
            menuItemList.setVisible(canChangeListLayout && listLayout != ArtworkListLayout.list);
            menuItemGrid.setVisible(canChangeListLayout && listLayout != ArtworkListLayout.grid);
        }
    }


    public static void register(Activity activity) {
        final Intent intent = new Intent(activity, JiveItemListActivity.class);
        intent.putExtra("register", true);
        activity.startActivity(intent);
    }

    /**
     * Start a new {@link JiveItemListActivity} to perform the supplied <code>action</code>.
     * <p>
     * If the action requires input, we initially get the input.
     * <p>
     * When input is ready or the action does not require input, items are ordered asynchronously
     * via {@link ISqueezeService#pluginItems(int, JiveItem, Action, IServiceItemListCallback)}
     *
     * @see #orderPage(ISqueezeService, int)
     */
    public static void show(Activity activity, JiveItem parent, Action action) {
        final Intent intent = getPluginListIntent(activity);
        intent.putExtra(JiveItem.class.getName(), parent);
        intent.putExtra(Action.class.getName(), action);
        activity.startActivityForResult(intent, GO);
    }

    public static void show(Activity activity, JiveItem item) {
        final Intent intent = getPluginListIntent(activity);
        intent.putExtra(JiveItem.class.getName(), item);
        activity.startActivityForResult(intent, GO);
    }

    @NonNull
    private static Intent getPluginListIntent(Activity activity) {
        Intent intent = new Intent(activity, JiveItemListActivity.class);
        if (activity instanceof JiveItemListActivity && ((JiveItemListActivity)activity).register) {
            intent.putExtra("register", true);
        }
        return intent;
    }

}
