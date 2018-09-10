package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.EnumWithTextAndIcon;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.VersionedEnumWithText;
import uk.org.ngo.squeezer.menu.ViewMenuItemFragment;
import uk.org.ngo.squeezer.service.ServerVersion;
import uk.org.ngo.squeezer.util.Reflection;

public abstract class BaseViewDialog<
        T extends Item,
        ListLayout extends Enum<ListLayout> & EnumWithTextAndIcon,
        SortOrder extends Enum<SortOrder> & VersionedEnumWithText> extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressWarnings("unchecked") final Class<ListLayout> listLayoutClass = (Class<ListLayout>) Reflection.getGenericClass(getClass(), BaseViewDialog.class, 1);
        @SuppressWarnings("unchecked") final Class<SortOrder> sortOrderClass = (Class<SortOrder>) Reflection.getGenericClass(getClass(), BaseViewDialog.class, 2);
        @SuppressWarnings("unchecked") final ViewMenuItemFragment.ListActivityWithViewMenu<T, ListLayout, SortOrder> activity = (ViewMenuItemFragment.ListActivityWithViewMenu<T, ListLayout, SortOrder>) getActivity();
        final int positionSortLabel = listLayoutClass.getEnumConstants().length;
        final int positionSortStart = positionSortLabel + 1;
        Bundle args = getArguments();
        final ServerVersion serverVersion = new ServerVersion(
                args != null ? args.getString("version") : "1");
        final List<SortOrder> sortOrders = getAvailableSortOrders(sortOrderClass.getEnumConstants(), serverVersion);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getTitle());
        builder.setAdapter(new BaseAdapter() {
                               @Override
                               public boolean areAllItemsEnabled() {
                                   return false;
                               }

                               @Override
                               public boolean isEnabled(int position) {
                                   return (position != positionSortLabel);
                               }

                               @Override
                               public int getCount() {
                                   return listLayoutClass.getEnumConstants().length + 1 + sortOrders.size();
                               }

                               @Override
                               public Object getItem(int i) {
                                   return null;
                               }

                               @Override
                               public long getItemId(int i) {
                                   return i;
                               }


                               @Override
                               public View getView(int position, View convertView,
                                                   ViewGroup parent) {
                                   if (position < positionSortLabel) {
                                       CheckedTextView textView = (CheckedTextView) getActivity()
                                               .getLayoutInflater()
                                               .inflate(android.R.layout.select_dialog_singlechoice,
                                                       parent, false);
                                       ListLayout listLayout = listLayoutClass.getEnumConstants()[position];
                                       textView.setCompoundDrawablesWithIntrinsicBounds(
                                               getIcon(listLayout), 0, 0, 0);
                                       textView.setText(listLayout.getText(getActivity()));
                                       textView.setChecked(listLayout == activity.getListLayout());
                                       return textView;
                                   } else if (position > positionSortLabel) {
                                       CheckedTextView textView = (CheckedTextView) getActivity()
                                               .getLayoutInflater()
                                               .inflate(android.R.layout.select_dialog_singlechoice,
                                                       parent, false);
                                       position -= positionSortStart;
                                       SortOrder sortOrder = sortOrders.get(position);
                                       textView.setText(sortOrder.getText(getActivity()));
                                       textView.setChecked(sortOrder == activity.getSortOrder());
                                       return textView;
                                   }

                                   TextView textView = new TextView(getActivity(), null,
                                           android.R.attr.listSeparatorTextViewStyle);
                                   textView.setText(getString(R.string.choose_sort_order,
                                           activity.getItemAdapter().getQuantityString(2)));
                                   return textView;
                               }
                           }, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int position) {
                                   if (position < positionSortLabel) {
                                       activity.setListLayout(listLayoutClass.getEnumConstants()[position]);
                                       dialog.dismiss();
                                   } else if (position > positionSortLabel) {
                                       position -= positionSortStart;
                                       activity.setSortOrder(sortOrders.get(position));
                                       dialog.dismiss();
                                   }
                               }
                           }
        );
        return builder.create();
    }

    private List<SortOrder> getAvailableSortOrders(SortOrder[] sortOrders, ServerVersion version) {
        List<SortOrder> availableSortOrders = new ArrayList<SortOrder>();
        for (SortOrder sortOrder : sortOrders)
            if (sortOrder.can(version)) availableSortOrders.add(sortOrder);
        return availableSortOrders;
    }

    protected int getIcon(ListLayout listLayout) {
        TypedValue v = new TypedValue();
        getActivity().getTheme().resolveAttribute(listLayout.getIconAttribute(), v, true);
        return v.resourceId;
    }

    protected abstract String getTitle();

}
