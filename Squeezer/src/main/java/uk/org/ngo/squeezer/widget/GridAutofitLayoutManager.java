package uk.org.ngo.squeezer.widget;

import android.content.Context;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Grid layout manager which takes a column width in stead of a span count.
 */
public class GridAutofitLayoutManager extends GridLayoutManager
{
    private int columnWidth = 0;
    private boolean columnWidthChanged;
    private int lastWidth = 0;
    private int lastHeight = 0;

    public GridAutofitLayoutManager(@NonNull final Context context, @DimenRes final int columnWidth) {
        /* Initially set spanCount to 1, will be changed automatically later. */
        super(context, 1);
        setColumnWidth(context, columnWidth);
    }

    public GridAutofitLayoutManager(
            @NonNull final Context context,
            @DimenRes final int columnWidth,
            final int orientation,
            final boolean reverseLayout) {

        /* Initially set spanCount to 1, will be changed automatically later. */
        super(context, 1, orientation, reverseLayout);
        setColumnWidth(context, columnWidth);
    }

    public void setColumnWidth(Context context, @DimenRes final int columnWidthDimension) {
        final int newColumnWidth = context.getResources().getDimensionPixelSize(columnWidthDimension);
        if (newColumnWidth != columnWidth) {
            columnWidth = newColumnWidth;
            columnWidthChanged = true;
        }
    }

    @Override
    public void onLayoutChildren(@NonNull final RecyclerView.Recycler recycler, @NonNull final RecyclerView.State state) {
        final int width = getWidth();
        final int height = getHeight();
        if (width > 0 && height > 0 && (columnWidthChanged || lastWidth != width || lastHeight != height)) {
            final int totalSpace;
            if (getOrientation() == VERTICAL) {
                totalSpace = width - getPaddingRight() - getPaddingLeft();
            } else {
                totalSpace = height - getPaddingTop() - getPaddingBottom();
            }
            final int spanCount = Math.max(1, totalSpace / columnWidth);
            setSpanCount(spanCount);
            columnWidthChanged = false;
        }
        lastWidth = width;
        lastHeight = height;
        super.onLayoutChildren(recycler, state);
    }
}

