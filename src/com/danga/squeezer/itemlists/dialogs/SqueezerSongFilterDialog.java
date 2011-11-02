package com.danga.squeezer.itemlists.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.danga.squeezer.R;
import com.danga.squeezer.itemlists.SqueezerSongListActivity;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.model.SqueezerYear;

public class SqueezerSongFilterDialog extends SqueezerBaseFilterDialog {
    private final SqueezerSongListActivity activity;
    private Spinner genreSpinnerView;
    private Spinner yearSpinnerView;
    private EditText editText;
    
    private SqueezerSongFilterDialog(SqueezerSongListActivity activity) {
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

        if (activity.getArtist() != null) {
            ((EditText)filterForm.findViewById(R.id.artist)).setText(activity.getArtist().getName());
            filterForm.findViewById(R.id.artist_view).setVisibility(View.VISIBLE);
        }
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
        activity.setYear((SqueezerYear) yearSpinnerView.getSelectedItem());
        activity.orderItems();
    }

    public static void addTo(SqueezerSongListActivity activity) {
        SqueezerSongFilterDialog dialog = new SqueezerSongFilterDialog(activity);
        dialog.show(activity.getSupportFragmentManager(), "FilterDialog");
    }

}
