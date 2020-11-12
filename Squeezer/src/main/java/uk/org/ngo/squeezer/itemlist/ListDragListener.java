/*
 * Copyright (c) 2020 Kurt Aaholst <kaaholst@gmail.com>
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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.DragEvent;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import uk.org.ngo.squeezer.R;

class ListDragListener extends Handler implements View.OnDragListener {
    private static final int MSG_SCROLL_ENDED = 1;

    private final CurrentPlaylistActivity activity;
    private int viewPosition;
    private int itemPosition;
    private boolean scrolling;
    private int scrollSpeed;
    private float lastMoveY = -1;

    public ListDragListener(CurrentPlaylistActivity activity) {
        super(Looper.getMainLooper());
        this.activity = activity;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch(event.getAction()) {

            case DragEvent.ACTION_DRAG_STARTED:
                itemPosition = viewPosition = getPosition(event);
                return true;

            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DROP:
                return true;

            case DragEvent.ACTION_DRAG_LOCATION: {
                int position = getPosition(event);
                // Move the highlighted song if necessary
                // Don't move to a position that is not yet loaded, because it will be overridden on load.
                if (position != AdapterView.INVALID_POSITION && position != viewPosition && activity.getItemAdapter().getItem(position) != null) {
                    // Prevent moving back if we have just swapped the current drag item with a taller item
                    if (lastMoveY == -1 || !((event.getY() > lastMoveY && position < viewPosition) || (event.getY() < lastMoveY && position > viewPosition))) {
                        int selectedIndex = activity.getSelectedIndex();
                        if (selectedIndex == viewPosition) {
                            activity.setSelectedIndex(position);
                        } else if (viewPosition < selectedIndex && position >= selectedIndex) {
                            activity.setSelectedIndex(selectedIndex - 1);
                        } else if (viewPosition > selectedIndex && position <= selectedIndex) {
                            activity.setSelectedIndex(selectedIndex + 1);
                        }

                        activity.getItemAdapter().moveItem(viewPosition, position);
                        activity.setDraggedIndex(position);
                        viewPosition = position;
                        lastMoveY = event.getY();
                    }
                }
                setScrollSpeed(event);
                return true;
            }

            case DragEvent.ACTION_DRAG_ENDED:
                activity.setDraggedIndex(-1);
                scrollSpeed = 0;
                lastMoveY = -1;
                if (viewPosition != itemPosition) {
                    activity.getService().playlistMove(itemPosition, viewPosition);
                    activity.skipPlaylistChanged();
                }
                return true;
        }

        return false;
    }

    private void startScroll(int scrollSpeed) {
        scrolling = true;
        lastMoveY = -1;
        int distance = activity.getResources().getDimensionPixelSize(R.dimen.playlist_scroll_distance);
        int duration = 250 - Math.abs(scrollSpeed);
        activity.getListView().smoothScrollBy(0, scrollSpeed < 0 ? -distance : distance, null, duration);
        removeMessages(MSG_SCROLL_ENDED);
        sendEmptyMessageDelayed(MSG_SCROLL_ENDED, duration-10);
    }

    private void setScrollSpeed(DragEvent event) {
        scrollSpeed = getScrollSpeed(event);
        if (scrollSpeed != 0 && !scrolling) {
            startScroll(scrollSpeed);
        }
    }

    private int getPosition(DragEvent event) {
        RecyclerView listView = activity.getListView();
        View childView = listView.findChildViewUnder((int) (event.getX()), (int) (event.getY()));
        return listView.getChildAdapterPosition(childView);
    }

    private int getScrollSpeed(DragEvent event) {
        int perMille = (int) (event.getY() / activity.getListView().getHeight() * 1000);
        return (perMille < 200 ? (perMille - 200) : perMille > 800 ? (perMille - 800) : 0);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        scrolling = false;
        if (scrollSpeed != 0) {
            startScroll(scrollSpeed);
        }
    }
}
