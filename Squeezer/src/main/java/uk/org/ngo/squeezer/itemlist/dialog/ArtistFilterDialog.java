package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.ArtistListActivity;
import uk.org.ngo.squeezer.itemlist.GenreSpinner;
import uk.org.ngo.squeezer.model.Genre;

public class ArtistFilterDialog extends BaseFilterDialog {

    private ArtistListActivity activity;

    private EditText editText;

    private Spinner genreSpinnerView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (ArtistListActivity) getActivity();
        editText = (EditText) filterForm.findViewById(R.id.search_string);
        editText.setHint(getString(R.string.filter_text_hint,
                activity.getItemAdapter().getQuantityString(2)));
        editText.setText(activity.getSearchString());

        filterForm.findViewById(R.id.year_view).setVisibility(View.GONE);
        genreSpinnerView = (Spinner) filterForm.findViewById(R.id.genre_spinner);
        new GenreSpinner(activity, activity, genreSpinnerView);

        if (activity.getAlbum() != null) {
            ((EditText) filterForm.findViewById(R.id.album)).setText(activity.getAlbum().getName());
            filterForm.findViewById(R.id.album_view).setVisibility(View.VISIBLE);
        }

        return dialog;
    }

    @Override
    protected void filter() {
        activity.setSearchString(editText.getText().toString());
        activity.setGenre((Genre) genreSpinnerView.getSelectedItem());
        activity.clearAndReOrderItems();
    }

}
