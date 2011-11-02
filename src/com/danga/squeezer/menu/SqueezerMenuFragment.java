package com.danga.squeezer.menu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.danga.squeezer.R;
import com.danga.squeezer.SettingsActivity;
import com.danga.squeezer.SqueezerActivity;
import com.danga.squeezer.SqueezerHomeActivity;


public class SqueezerMenuFragment extends Fragment {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.itemlistmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_home:
            SqueezerHomeActivity.show(getActivity());
            return true;
        case R.id.menu_item_main:
            SqueezerActivity.show(getActivity());
            return true;
        case R.id.menu_item_settings:
            SettingsActivity.show(getActivity());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public static void addTo(FragmentActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(0, new SqueezerMenuFragment());
        fragmentTransaction.commit();
    }

}
