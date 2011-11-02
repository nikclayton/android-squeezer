package com.danga.squeezer.itemlists.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.danga.squeezer.R;
import com.danga.squeezer.itemlists.SqueezerAlbumListActivity;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerYear;

public class SqueezerAlbumFilterDialog extends SqueezerBaseFilterDialog {
    private final SqueezerAlbumListActivity activity;
    private Spinner genreSpinnerView;
    private Spinner yearSpinnerView;
    private EditText editText;
    
    private SqueezerAlbumFilterDialog(SqueezerAlbumListActivity activity) {
        this.activity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        editText = (EditText) filterForm.findViewById(R.id.search_string);
        editText.setHint(getString(R.string.filter_text_hint, activity.getItemAdapter().getQuantityString(2)));
        editText.setText(activity.getSearchString());

        genreSpinnerView = (Spinner) filterForm.findViewById(R.id.genre_spinner);
        yearSpinnerView = (Spinner) filterForm.findViewById(R.id.year_spinner);
        activity.setGenreSpinner(genreSpinnerView);
        activity.setYearSpinner(yearSpinnerView);

        if (activity.getSong() != null) {
            ((EditText)filterForm.findViewById(R.id.track)).setText(activity.getSong().getName());
            filterForm.findViewById(R.id.track_view).setVisibility(View.VISIBLE);
        }
        if (activity.getArtist() != null) {
            ((EditText)filterForm.findViewById(R.id.artist)).setText(activity.getArtist().getName());
            filterForm.findViewById(R.id.artist_view).setVisibility(View.VISIBLE);
        }

        return dialog;
    }

    @Override
    protected void filter() {
        activity.setSearchString(editText.getText().toString());
        activity.setGenre((SqueezerGenre) genreSpinnerView.getSelectedItem());
        activity.setYear((SqueezerYear) yearSpinnerView.getSelectedItem());
        activity.orderItems();
    }

    public static void addTo(SqueezerAlbumListActivity activity) {
        SqueezerAlbumFilterDialog dialog = new SqueezerAlbumFilterDialog(activity);
        dialog.show(activity.getSupportFragmentManager(), "FilterDialog");
    }

}
