package com.danga.squeezer.menu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.danga.squeezer.R;


public class SqueezerFilterMenuItemFragment extends Fragment {
    final SqueezerFilterableListActivity activity;
    
    private SqueezerFilterMenuItemFragment(SqueezerFilterableListActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    };
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filtermenuitem, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_filter:
            activity.showFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public static void addTo(SqueezerFilterableListActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(0, new SqueezerFilterMenuItemFragment(activity));
        fragmentTransaction.commit();
    }
    
    public interface SqueezerFilterableListActivity {
        public void showFilterDialog();
        FragmentManager getSupportFragmentManager();
    }

}
