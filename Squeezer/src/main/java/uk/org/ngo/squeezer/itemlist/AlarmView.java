/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.google.common.collect.ImmutableList;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;
import uk.org.ngo.squeezer.widget.AnimationEndListener;
import uk.org.ngo.squeezer.widget.UndoBarController;

public class AlarmView extends BaseItemView<Alarm> {
    private static final int ANIMATION_DURATION = 300;

    private final AlarmsActivity mActivity;
    private final Resources mResources;
    private final int mColorSelected;
    private final float mDensity;
    private final List<AlarmPlaylist> mAlarmPlaylists = new ArrayList<>();

    public AlarmView(AlarmsActivity activity) {
        super(activity);
        mActivity = activity;
        mResources = activity.getResources();
        mColorSelected = mResources.getColor(getActivity().getAttributeValue(R.attr.alarm_dow_selected));
        mDensity = mResources.getDisplayMetrics().density;
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, int position, Alarm item) {
        View view = getAdapterView(convertView, parent);
        bindView((AlarmViewHolder) view.getTag(), position, item);
        return view;
    }

    private View getAdapterView(View convertView, final ViewGroup parent) {
        AlarmViewHolder currentViewHolder =
                (convertView != null && convertView.getTag() instanceof AlarmViewHolder)
                        ? (AlarmViewHolder) convertView.getTag()
                        : null;

        if (currentViewHolder == null) {
            convertView = getLayoutInflater().inflate(R.layout.list_item_alarm, parent, false);
            final View alarmView = convertView;
            final AlarmViewHolder viewHolder = new AlarmViewHolder();
            viewHolder.is24HourFormat = DateFormat.is24HourFormat(getActivity());
            viewHolder.timeFormat = viewHolder.is24HourFormat ? "%02d:%02d" : "%d:%02d";
            String[] amPmStrings = new DateFormatSymbols().getAmPmStrings();
            viewHolder.am = amPmStrings[0];
            viewHolder.pm = amPmStrings[1];
            viewHolder.time = convertView.findViewById(R.id.time);
            viewHolder.amPm = convertView.findViewById(R.id.am_pm);
            viewHolder.amPm.setVisibility(viewHolder.is24HourFormat ? View.GONE : View.VISIBLE);
            viewHolder.enabled = new CompoundButtonWrapper((CompoundButton) convertView.findViewById(R.id.enabled));
            viewHolder.enabled.setOncheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (getActivity().getService() != null) {
                        viewHolder.alarm.setEnabled(b);
                        getActivity().getService().alarmEnable(viewHolder.alarm.getId(), b);
                    }
                }
            });
            viewHolder.repeat = new CompoundButtonWrapper((CompoundButton) convertView.findViewById(R.id.repeat));
            viewHolder.repeat.setOncheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (getActivity().getService() != null) {
                        viewHolder.alarm.setRepeat(b);
                        getActivity().getService().alarmRepeat(viewHolder.alarm.getId(), b);
                        viewHolder.dowHolder.setVisibility(b ? View.VISIBLE : View.GONE);
                    }
                }
            });
            viewHolder.repeat.getButton().setText(R.string.ALARM_ALARM_REPEAT);
            viewHolder.delete = convertView.findViewById(R.id.delete);
            viewHolder.playlist = convertView.findViewById(R.id.playlist);
            viewHolder.dowHolder = convertView.findViewById(R.id.dow);
            for (int day = 0; day < 7; day++) {
                ViewGroup dowButton = (ViewGroup) viewHolder.dowHolder.getChildAt(day);
                final int finalDay = day;
                dowButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getActivity().getService() != null) {
                            final Alarm alarm = viewHolder.alarm;
                            boolean wasChecked = alarm.isDayActive(finalDay);
                            if (wasChecked) {
                                alarm.clearDay(finalDay);
                                getActivity().getService().alarmRemoveDay(alarm.getId(), finalDay);
                            } else {
                                alarm.setDay(finalDay);
                                getActivity().getService().alarmAddDay(alarm.getId(), finalDay);
                            }
                            setDowText(viewHolder, finalDay);
                        }
                    }
                });
                viewHolder.dowTexts[day] = (TextView) dowButton.getChildAt(0);
            }
            viewHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final AnimationSet animationSet = new AnimationSet(true);
                    animationSet.addAnimation(new ScaleAnimation(1F, 1F, 1F, 0.5F));
                    animationSet.addAnimation(new AlphaAnimation(1F, 0F));
                    animationSet.setDuration(ANIMATION_DURATION);
                    animationSet.setAnimationListener(new AnimationEndListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mActivity.getItemAdapter().removeItem(viewHolder.position);
                            UndoBarController.show(getActivity(), R.string.ALARM_DELETING, new UndoListener(viewHolder.position, viewHolder.alarm));
                        }
                    });

                    alarmView.startAnimation(animationSet);
                }
            });
            viewHolder.playlist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    final AlarmPlaylist selectedAlarmPlaylist = mAlarmPlaylists.get(position);
                    final Alarm alarm = viewHolder.alarm;
                    if (getActivity().getService() != null &&
                            selectedAlarmPlaylist.getId() != null &&
                            !selectedAlarmPlaylist.getId().equals(alarm.getUrl())) {
                        alarm.setUrl(selectedAlarmPlaylist.getId());
                        getActivity().getService().alarmSetPlaylist(alarm.getId(), selectedAlarmPlaylist);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            convertView.setTag(viewHolder);
        }

        return convertView;
    }

    private void bindView(final AlarmViewHolder viewHolder, final int position, final Alarm item) {
        long tod = item.getTod();
        int hour = (int) (tod / 3600);
        int minute = (int) ((tod / 60) % 60);
        int displayHour = hour;
        if (!viewHolder.is24HourFormat) {
            displayHour = displayHour % 12;
            if (displayHour == 0) displayHour = 12;
        }

        viewHolder.position = position;
        viewHolder.alarm = item;
        viewHolder.time.setText(String.format(viewHolder.timeFormat, displayHour, minute));
        viewHolder.time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerFragment.show(getActivity().getSupportFragmentManager(), item, viewHolder.is24HourFormat, getActivity().getThemeId() == R.style.AppTheme);
            }
        });
        viewHolder.amPm.setText(hour < 12 ? viewHolder.am : viewHolder.pm);
        viewHolder.enabled.setChecked(item.isEnabled());
        viewHolder.repeat.setChecked(item.isRepeat());
        if (!mAlarmPlaylists.isEmpty()) {
            viewHolder.playlist.setAdapter(new AlarmPlaylistSpinnerAdapter());
            for (int i = 0; i < mAlarmPlaylists.size(); i++) {
                AlarmPlaylist alarmPlaylist = mAlarmPlaylists.get(i);
                if (alarmPlaylist.getId() != null && alarmPlaylist.getId().equals(item.getUrl())) {
                    viewHolder.playlist.setSelection(i);
                    break;
                }
            }

        }

        viewHolder.dowHolder.setVisibility(item.isRepeat() ? View.VISIBLE : View.GONE);
        for (int day = 0; day < 7; day++) {
            setDowText(viewHolder, day);
        }
    }

    private void setDowText(AlarmViewHolder viewHolder, int day) {
        SpannableString text = new SpannableString(getAlarmShortDayText(day));
        if (viewHolder.alarm.isDayActive(day)) {
            text.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
            text.setSpan(new ForegroundColorSpan(mColorSelected), 0, text.length(), 0);
            Drawable underline = mResources.getDrawable(R.drawable.underline);
            float textSize = (new Paint()).measureText(text.toString());
            underline.setBounds(0, 0, (int) (textSize * mDensity), (int) (1 * mDensity));
            viewHolder.dowTexts[day].setCompoundDrawables(null, null, null, underline);
        } else
            viewHolder.dowTexts[day].setCompoundDrawables(null, null, null, null);
        viewHolder.dowTexts[day].setText(text);
    }

    private CharSequence getAlarmShortDayText(int day) {
        switch (day) {
            default: return getActivity().getString(R.string.ALARM_SHORT_DAY_0);
            case 1: return getActivity().getString(R.string.ALARM_SHORT_DAY_1);
            case 2: return getActivity().getString(R.string.ALARM_SHORT_DAY_2);
            case 3: return getActivity().getString(R.string.ALARM_SHORT_DAY_3);
            case 4: return getActivity().getString(R.string.ALARM_SHORT_DAY_4);
            case 5: return getActivity().getString(R.string.ALARM_SHORT_DAY_5);
            case 6: return getActivity().getString(R.string.ALARM_SHORT_DAY_6);
        }
    }

    @Override
    public boolean isSelectable(Alarm item) {
        return false;
    }

    @Override
    public void onItemSelected(View view, int index, Alarm item) {
    }
    public void onGroupSelected(View view, Alarm[] items) {
    
    }
    // Require an immutable list so that caller's can't modify it when this method iterates
    // over it.
    public void setAlarmPlaylists(ImmutableList<AlarmPlaylist> alarmPlaylists) {
        String currentCategory = null;

        mAlarmPlaylists.clear();
        for (AlarmPlaylist alarmPlaylist : alarmPlaylists) {
            if (!alarmPlaylist.getCategory().equals(currentCategory)) {
                AlarmPlaylist categoryAlarmPlaylist = new AlarmPlaylist();
                categoryAlarmPlaylist.setCategory(alarmPlaylist.getCategory());
                mAlarmPlaylists.add(categoryAlarmPlaylist);
            }
            mAlarmPlaylists.add(alarmPlaylist);
            currentCategory = alarmPlaylist.getCategory();
        }
    }

    private static class AlarmViewHolder {
        int position;
        public boolean is24HourFormat;
        String timeFormat;
        String am;
        String pm;
        Alarm alarm;
        TextView time;
        TextView amPm;
        CompoundButtonWrapper enabled;
        CompoundButtonWrapper repeat;
        ImageView delete;
        Spinner playlist;
        LinearLayout dowHolder;
        final TextView[] dowTexts = new TextView[7];
    }

    public static class TimePickerFragment extends TimePickerDialog implements TimePickerDialog.OnTimeSetListener {
        BaseListActivity activity;
        Alarm alarm;

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            activity = (BaseListActivity) getActivity();
            alarm = getArguments().getParcelable("alarm");
            setOnTimeSetListener(this);
            return super.onCreateDialog(savedInstanceState);
        }

        public static void show(FragmentManager manager, Alarm alarm, boolean is24HourFormat, boolean dark) {
            long tod = alarm.getTod();
            int hour = (int) (tod / 3600);
            int minute = (int) ((tod / 60) % 60);

            TimePickerFragment fragment = new TimePickerFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("alarm", alarm);
            fragment.setArguments(bundle);
            fragment.initialize(fragment, hour, minute, is24HourFormat);
            fragment.setThemeDark(dark);
            fragment.show(manager, TimePickerFragment.class.getSimpleName());
        }

        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
            if (activity.getService() != null) {
                int time = (hourOfDay * 60 + minute) * 60;
                alarm.setTod(time);
                activity.getService().alarmSetTime(alarm.getId(), time);
                activity.getItemAdapter().notifyDataSetChanged();
            }
        }
    }

    private class AlarmPlaylistSpinnerAdapter extends ArrayAdapter<AlarmPlaylist> {

        public AlarmPlaylistSpinnerAdapter() {
            super(getActivity(), android.R.layout.simple_spinner_dropdown_item, mAlarmPlaylists);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return (mAlarmPlaylists.get(position).getId() != null);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
           return Util.getSpinnerItemView(getActivity(), convertView, parent, getItem(position).getName());
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (!isEnabled(position)) {
                FrameLayout view = (FrameLayout) getActivity().getLayoutInflater().inflate(R.layout.alarm_playlist_category_dropdown_item, parent, false);
                CheckedTextView spinnerItemView = view.findViewById(R.id.text);
                spinnerItemView.setText(getItem(position).getCategory());
                spinnerItemView.setTypeface(spinnerItemView.getTypeface(), Typeface.BOLD);
                // Hide the checkmark for headings.
                spinnerItemView.setCheckMarkDrawable(new ColorDrawable(Color.TRANSPARENT));
                return view;
            } else {
                FrameLayout view = (FrameLayout) getActivity().getLayoutInflater().inflate(R.layout.alarm_playlist_dropdown_item, parent, false);
                TextView spinnerItemView = view.findViewById(R.id.text);
                spinnerItemView.setText(getItem(position).getName());
                return view;
            }
        }
    }

    private class UndoListener implements UndoBarController.UndoListener {
        private final int position;
        private final Alarm alarm;

        public UndoListener(int position, Alarm alarm) {
            this.position = position;
            this.alarm = alarm;
        }

        @Override
        public void onUndo() {
            mActivity.getItemAdapter().insertItem(position, alarm);
        }

        @Override
        public void onDone() {
            if (mActivity.getService() != null) {
                mActivity.getService().alarmDelete(alarm.getId());
            }
        }
    }
}
