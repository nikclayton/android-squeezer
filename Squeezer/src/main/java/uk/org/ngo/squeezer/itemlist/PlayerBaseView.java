package uk.org.ngo.squeezer.itemlist;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;

import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.model.Player;


public abstract class PlayerBaseView<A extends PlayerListBaseActivity> extends BaseItemView<Player> {
    protected final A activity;
    private @LayoutRes
    int layoutResource;

    public PlayerBaseView(A activity, @LayoutRes int layoutResource) {
        super(activity);
        this.activity = activity;
        this.layoutResource = layoutResource;
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, @ViewParam int viewParams) {
        return getAdapterView(convertView, parent, viewParams, layoutResource);
    }

    @Override
    public void onItemSelected(View view, int index, Player item) {
    }

    public void onGroupSelected(View view, Player[] items) {

    }

    public void onGroupSelected(View view) {

    }
}
