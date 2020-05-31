package uk.org.ngo.squeezer.itemlist;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;

import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.model.Player;


public abstract class PlayerBaseView<A extends PlayerListBaseActivity> extends BaseItemView<Player> {
    private static final Map<String, Integer> modelIcons = PlayerBaseView.initializeModelIcons();
    protected final A activity;
    private @LayoutRes
    int layoutResource;

    public PlayerBaseView(A activity, @LayoutRes int layoutResource) {
        super(activity);
        this.activity = activity;
        this.layoutResource = layoutResource;
    }

    private static Map<String, Integer> initializeModelIcons() {
        Map<String, Integer> modelIcons = new HashMap<>();
        modelIcons.put("baby", R.drawable.ic_baby);
        modelIcons.put("boom", R.drawable.ic_boom);
        modelIcons.put("fab4", R.drawable.ic_fab4);
        modelIcons.put("receiver", R.drawable.ic_receiver);
        modelIcons.put("controller", R.drawable.ic_controller);
        modelIcons.put("sb1n2", R.drawable.ic_sb1n2);
        modelIcons.put("sb3", R.drawable.ic_sb3);
        modelIcons.put("slimp3", R.drawable.ic_slimp3);
        modelIcons.put("softsqueeze", R.drawable.ic_softsqueeze);
        modelIcons.put("squeezeplay", R.drawable.ic_squeezeplay);
        modelIcons.put("transporter", R.drawable.ic_transporter);
        modelIcons.put("squeezeplayer", R.drawable.ic_squeezeplayer);
        return modelIcons;
    }

    protected static int getModelIcon(String model) {
        Integer icon = modelIcons.get(model);
        return (icon != null ? icon : R.drawable.ic_blank);
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
