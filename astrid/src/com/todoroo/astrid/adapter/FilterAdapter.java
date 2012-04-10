/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterCategoryWithNewButton;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;

public class FilterAdapter extends ArrayAdapter<Filter> {

    public static interface FilterDataSourceChangedListener {
        public void filterDataSourceChanged();
    }

    // --- style constants

    public int filterStyle = R.style.TextAppearance_FLA_Filter;
    public int headerStyle = R.style.TextAppearance_FLA_Header;

    // --- instance variables

    @Autowired
    private TaskService taskService;

    /** parent activity */
    protected final Activity activity;

    /** owner listview */
    protected ListView listView;

    /** display metrics for scaling icons */
    private final DisplayMetrics metrics = new DisplayMetrics();

    /** receiver for new filters */
    private final FilterReceiver filterReceiver = new FilterReceiver();
    private final BladeFilterReceiver bladeFilterReceiver = new BladeFilterReceiver();
    private boolean shouldUseBladeFilter = true;

    /** row layout to inflate */
    private final int layout;

    /** layout inflater */
    private final LayoutInflater inflater;

    /** whether to skip Filters that launch intents instead of being real filters */
    private final boolean skipIntentFilters;

    /** whether rows are selectable */
    private final boolean selectable;

    /** Pattern for matching filter counts in listing titles */
    private final Pattern countPattern = Pattern.compile(".* \\((\\d+)\\)$"); //$NON-NLS-1$

    private int mSelectedIndex;

    private final HashMap<Filter, Integer> filterCounts;

    private FilterDataSourceChangedListener listener;


    // Previous solution involved a queue of filters and a filterSizeLoadingThread. The filterSizeLoadingThread had
    // a few problems: how to make sure that the thread is resumed when the controlling activity is resumed, and
    // how to make sure that the the filterQueue does not accumulate filters without being processed. I am replacing
    // both the queue and a the thread with a thread pool, which will shut itself off after a second if it has
    // nothing to do (corePoolSize == 0, which makes it available for garbage collection), and will wake itself up
    // if new filters are queued (obviously it cannot be garbage collected if it is possible for new filters to
    // be added).
    private final ThreadPoolExecutor filterExecutor = new ThreadPoolExecutor(0, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    public FilterAdapter(Activity activity, ListView listView,
            int rowLayout, boolean skipIntentFilters) {
        this(activity, listView, rowLayout, skipIntentFilters, false);
    }

    public FilterAdapter(Activity activity, ListView listView,
            int rowLayout, boolean skipIntentFilters, boolean selectable) {
        super(activity, 0);

        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        this.listView = listView;
        this.layout = rowLayout;
        this.skipIntentFilters = skipIntentFilters;
        this.selectable = selectable;
        this.filterCounts = new HashMap<Filter, Integer>();

        if (activity instanceof AstridActivity && ((AstridActivity) activity).getFragmentLayout() != AstridActivity.LAYOUT_SINGLE)
            filterStyle = R.style.TextAppearance_FLA_Filter_Tablet;

        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    private void offerFilter(final Filter filter) {
        if(selectable && selection == null)
            setSelection(filter);
        filterExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int size = -1;
                    Matcher m = countPattern.matcher(filter.listingTitle);
                    if(m.find()) {
                        String countString = m.group(1);
                        try {
                            size = Integer.parseInt(countString);
                        } catch (NumberFormatException e) {
                            // Count manually
                            e.printStackTrace();
                        }
                    }

                    if (size < 0) {
                        size = taskService.countTasks(filter);
                        filter.listingTitle = filter.listingTitle + (" (" + //$NON-NLS-1$
                                size + ")"); //$NON-NLS-1$
                    }

                    filterCounts.put(filter, size);
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                    Log.e("astrid-filter-adapter", "Error loading filter size", e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        });
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void add(Filter item) {
        super.add(item);
        notifyDataSetChanged();
        // load sizes
        offerFilter(item);
    }

    public int addOrLookup(Filter filter) {
        int index = getPosition(filter);
        if (index >= 0) return index;
        add(filter);
        return getCount() - 1;
    }

    public void setLastSelected(int lastSelected) {
        mSelectedIndex = lastSelected;
    }

    public int adjustFilterCount(Filter filter, int delta) {
        int filterCount = 0;
        if (filterCounts.containsKey(filter)) {
            filterCount = filterCounts.get(filter);
        }
        int newCount = Math.max(filterCount + delta, 0);
        filterCounts.put(filter, newCount);
        notifyDataSetChanged();
        return newCount;
    }

    public int incrementFilterCount(Filter filter) {
        return adjustFilterCount(filter, 1);
    }

    public int decrementFilterCount(Filter filter) {
        return adjustFilterCount(filter, -1);
    }

    public void refreshFilterCount(final Filter filter) {
        filterExecutor.submit(new Runnable() {
            @Override
            public void run() {
                int size = taskService.countTasks(filter);
                filterCounts.put(filter, size);
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        });
    }

    public void setDataSourceChangedListener(FilterDataSourceChangedListener listener) {
        this.listener = listener;
    }

    public void setListView(ListView listView) {
        this.listView = listView;
    }

    /**
     * Create or reuse a view
     * @param convertView
     * @param parent
     * @return
     */
    protected View newView(View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(layout, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.view = convertView;
            viewHolder.icon = (ImageView)convertView.findViewById(R.id.icon);
            viewHolder.urlImage = (AsyncImageView)convertView.findViewById(R.id.url_image);
            viewHolder.name = (TextView)convertView.findViewById(R.id.name);
            viewHolder.selected = (ImageView)convertView.findViewById(R.id.selected);
            viewHolder.size = (TextView)convertView.findViewById(R.id.size);
            viewHolder.decoration = null;
            convertView.setTag(viewHolder);
        }
        return convertView;
    }

    public static class ViewHolder {
        public FilterListItem item;
        public ImageView icon;
        public AsyncImageView urlImage;
        public TextView name;
        public TextView size;
        public ImageView selected;
        public View view;
        public View decoration;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = newView(convertView, parent);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.item = (FilterListItem) getItem(position);
        populateView(viewHolder);

        if (listView.isItemChecked(position)) {
            convertView.setBackgroundColor(activity.getResources().getColor(R.color.tablet_list_selected));
        } else {
            convertView.setBackgroundColor(activity.getResources().getColor(android.R.color.transparent));
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
    /* ======================================================================
     * ============================================================ selection
     * ====================================================================== */

    private FilterListItem selection = null;

    /**
     * Sets the selected item to this one
     * @param picked
     */
    public void setSelection(FilterListItem picked) {
        selection = picked;
        int scroll = listView.getScrollY();
        notifyDataSetInvalidated();
        listView.scrollTo(0, scroll);
    }

    /**
     * Gets the currently selected item
     * @return null if no item is to be selected
     */
    public FilterListItem getSelection() {
        return selection;
    }

    /* ======================================================================
     * ============================================================= receiver
     * ====================================================================== */

    /**
     * Receiver which receives intents to add items to the filter list
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class FilterReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Bundle extras = intent.getExtras();
                extras.setClassLoader(FilterListHeader.class.getClassLoader());
                final Parcelable[] filters = extras.getParcelableArray(AstridApiConstants.EXTRAS_RESPONSE);
                populateFiltersToAdapter(filters);
                shouldUseBladeFilter = false;
            } catch (Exception e) {
                Log.e("receive-filter-" +  //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON),
                        e.toString(), e);
            }
        }

        protected void populateFiltersToAdapter(final Parcelable[] filters) {
            if (filters == null)
                return;

            for (Parcelable item : filters) {
                FilterListItem filter = (FilterListItem) item;
                if(skipIntentFilters && !(filter instanceof Filter ||
                            filter instanceof FilterListHeader ||
                            filter instanceof FilterCategory))
                    continue;
                onReceiveFilter((FilterListItem)item);

                if (filter instanceof FilterCategory) {
                    Filter[] children = ((FilterCategory) filter).children;
                    for (Filter f : children) {
                        addOrLookup(f);
                    }
                } else if (filter instanceof Filter){
                    addOrLookup((Filter) filter);
                }
            }

            if (mSelectedIndex < getCount()) {
                listView.setItemChecked(mSelectedIndex, true);
            }
            notifyDataSetChanged();
        }
    }

    /**
     * Receiver which gets called after the FilterReceiver
     * and checks if the filters are populated.
     * If they aren't (e.g. due to the bug in the ZTE Blade's Parcelable-system throwing a
     * ClassNotFoundExeption), the filters are fetched manually.
     *
     * @author Arne Jans <arne@astrid.com>
     *
     */
    public class BladeFilterReceiver extends FilterReceiver {
        private final List<ResolveInfo> filterExposerList;

        public BladeFilterReceiver() {
            // query astrids AndroidManifest.xml for all registered default-receivers to expose filters
            PackageManager pm = ContextManager.getContext().getPackageManager();
            filterExposerList = pm.queryBroadcastReceivers(
                    new Intent(AstridApiConstants.BROADCAST_REQUEST_FILTERS),
                    PackageManager.MATCH_DEFAULT_ONLY);
        }

        @SuppressWarnings("nls")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (shouldUseBladeFilter && getCount() == 0 && filterExposerList != null && filterExposerList.size()>0) {
                try {
                    for (ResolveInfo filterExposerInfo : filterExposerList) {
                        Log.d("BladeFilterReceiver", filterExposerInfo.toString());
                        String className = filterExposerInfo.activityInfo.name;
                        AstridFilterExposer filterExposer = null;
                            filterExposer = (AstridFilterExposer) Class.forName(className, true, FilterAdapter.class.getClassLoader()).newInstance();

                        if (filterExposer != null) {
                            populateFiltersToAdapter(filterExposer.getFilters());
                        }
                    }
                } catch (Exception e) {
                    Log.e("receive-bladefilter-" +  //$NON-NLS-1$
                            intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON),
                            e.toString(), e);
                }
            }
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (listener != null)
            listener.filterDataSourceChanged();
    }

    /**
     * Broadcast a request for lists. The request is sent to every
     * application registered to listen for this broadcast. Each application
     * can then add lists to this activity
     */
    public void getLists() {
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_FILTERS);
//        activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        // the bladeFilterReceiver will be called after the usual FilterReceiver has finished (to handle the empty list)
        activity.sendOrderedBroadcast(broadcastIntent,
                AstridApiConstants.PERMISSION_READ,
                bladeFilterReceiver,
                null,
                Activity.RESULT_OK,
                null,
                null);
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void registerRecevier() {
        IntentFilter regularFilter = new IntentFilter(AstridApiConstants.BROADCAST_SEND_FILTERS);
        regularFilter.setPriority(2);
        activity.registerReceiver(filterReceiver, regularFilter);
        IntentFilter bladeFilter = new IntentFilter(AstridApiConstants.BROADCAST_SEND_FILTERS);
        bladeFilter.setPriority(1);
        activity.registerReceiver(bladeFilterReceiver, bladeFilter);
        getLists();
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void unregisterRecevier() {
        activity.unregisterReceiver(filterReceiver);
        activity.unregisterReceiver(bladeFilterReceiver);
    }

    /**
     * Called when an item comes through. Override if you like
     * @param item
     */
    public void onReceiveFilter(FilterListItem item) {
        // do nothing
    }

    /* ======================================================================
     * ================================================================ views
     * ====================================================================== */

    public void populateView(ViewHolder viewHolder) {
        FilterListItem filter = viewHolder.item;
        if(filter == null)
            return;

        viewHolder.view.setBackgroundResource(0);

        if(viewHolder.decoration != null) {
            ((ViewGroup)viewHolder.view).removeView(viewHolder.decoration);
            viewHolder.decoration = null;
        }

        if(viewHolder.item instanceof FilterListHeader || viewHolder.item instanceof FilterCategory) {
            viewHolder.name.setTextAppearance(activity, headerStyle);
            viewHolder.name.setShadowLayer(1, 1, 1, Color.BLACK);
            viewHolder.view.setPadding((int) (7 * metrics.density), 5, 0, 5);
            viewHolder.view.getLayoutParams().height = (int) (40 * metrics.density);
        } else {
            viewHolder.name.setTextAppearance(activity, filterStyle);
            viewHolder.name.setShadowLayer(0, 0, 0, 0);
            viewHolder.view.setPadding((int) (7 * metrics.density), 2, 0, 2);
            viewHolder.view.getLayoutParams().height = (int) (58 * metrics.density);
        }

        // update with filter attributes (listing icon, url, update text, size)

        viewHolder.urlImage.setVisibility(View.GONE);
        viewHolder.icon.setVisibility(View.GONE);

        if(filter.listingIcon != null) {
            viewHolder.icon.setVisibility(View.VISIBLE);
            viewHolder.icon.setImageBitmap(filter.listingIcon);
        }

        // title / size
        if(filterCounts.containsKey(filter) || filter.listingTitle.matches(".* \\(\\d+\\)$")) { //$NON-NLS-1$
            viewHolder.size.setVisibility(View.VISIBLE);
            String count;
            if (filterCounts.containsKey(filter)) {
                count = filterCounts.get(filter).toString();
            } else {
                count = filter.listingTitle.substring(filter.listingTitle.lastIndexOf('(') + 1,
                        filter.listingTitle.length() - 1);
            }
            viewHolder.size.setText(count);

            String title;
            int listingTitleSplit = filter.listingTitle.lastIndexOf(' ');
            if (listingTitleSplit > 0) {
                title = filter.listingTitle.substring(0, listingTitleSplit);
            } else {
                title = filter.listingTitle;
            }
            viewHolder.name.setText(title);
        } else {
            viewHolder.name.setText(filter.listingTitle);
            viewHolder.size.setVisibility(View.GONE);
        }

        viewHolder.name.getLayoutParams().height = (int) (58 * metrics.density);
        if(filter instanceof FilterWithUpdate) {
            viewHolder.urlImage.setVisibility(View.VISIBLE);
            viewHolder.urlImage.setDefaultImageResource(TagService.getDefaultImageIDForTag(viewHolder.name.getText().toString()));
            viewHolder.urlImage.setUrl(((FilterWithUpdate)filter).imageUrl);
        }

        if(filter.color != 0)
            viewHolder.name.setTextColor(filter.color);

        // selection
        if(selection == viewHolder.item) {
            viewHolder.selected.setVisibility(View.VISIBLE);
            viewHolder.view.setBackgroundColor(Color.rgb(128, 230, 0));
        } else
            viewHolder.selected.setVisibility(View.GONE);

        if(filter instanceof FilterCategoryWithNewButton)
            setupCustomHeader(viewHolder, (FilterCategoryWithNewButton) filter);
    }

    private void setupCustomHeader(ViewHolder viewHolder, final FilterCategoryWithNewButton filter) {
        Button add = new Button(activity);
        add.setBackgroundResource(R.drawable.filter_btn_background);
        add.setCompoundDrawablesWithIntrinsicBounds(R.drawable.filter_new,0,0,0);
        add.setTextColor(Color.WHITE);
        add.setShadowLayer(1, 1, 1, Color.BLACK);
        add.setText(filter.label);
        add.setFocusable(false);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                (int)(32 * metrics.density));
        lp.rightMargin = (int) (4 * metrics.density);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        add.setLayoutParams(lp);
        ((ViewGroup)viewHolder.view).addView(add);
        viewHolder.decoration = add;

        add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            filter.intent.send(FilterListFragment.REQUEST_NEW_BUTTON, new PendingIntent.OnFinished() {
                                @Override
                                public void onSendFinished(PendingIntent pendingIntent, Intent intent,
                                        int resultCode, String resultData, Bundle resultExtras) {
                                    activity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            clear();
                                        }
                                    });
                                }
                            }, null);
                        } catch (CanceledException e) {
                            // do nothing
                        }
                    }
                });
    }
}
