package com.danga.squeezer.itemlists.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.danga.squeezer.R;
import com.danga.squeezer.itemlists.SqueezerArtistListActivity;
import com.danga.squeezer.model.SqueezerGenre;

public class SqueezerArtistFilterDialog extends SqueezerBaseFilterDialog {
    private final SqueezerArtistListActivity activity;
    private EditText editText;
    private Spinner genreSpinnerView;
    
    private SqueezerArtistFilterDialog(SqueezerArtistListActivity activity) {
        this.activity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        editText = (EditText) filterForm.findViewById(R.id.search_string);
        editText.setHint(getString(R.string.filter_text_hint, activity.getItemAdapter().getQuantityString(2)));
        editText.setText(activity.getSearchString());

        filterForm.findViewById(R.id.year_view).setVisibility(View.GONE);
        genreSpinnerView = (Spinner) filterForm.findViewById(R.id.genre_spinner);
        activity.setGenreSpinner(genreSpinnerView);

        if (activity.getAlbum() != null) {
            ((EditText) filterForm.findViewById(R.id.album)).setText(activity.getAlbum().getName());
            filterForm.findViewById(R.id.album_view).setVisibility(View.VISIBLE);
        }

        return dialog;
    }
    
    @Override
    protected void filter() {
        activity.setSearchString(editText.getText().toString());
        activity.setGenre((SqueezerGenre) genreSpinnerView.getSelectedItem());
        activity.orderItems();
    }

    public static void addTo(SqueezerArtistListActivity activity) {
        SqueezerArtistFilterDialog dialog = new SqueezerArtistFilterDialog(activity);
        dialog.show(activity.getSupportFragmentManager(), "FilterDialog");
    }

}
