package com.todoroo.astrid.adapter;

import greendroid.widget.AsyncImageView;
import greendroid.widget.QuickAction;
import greendroid.widget.QuickActionBar;
import greendroid.widget.QuickActionWidget;
import greendroid.widget.QuickActionWidget.OnQuickActionClickListener;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import android.app.ListFragment;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.api.TaskDecorationExposer;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.fragment.TaskEditFragment;
import com.todoroo.astrid.fragment.TaskListFragment;
import com.todoroo.astrid.helper.TaskAdapterAddOnManager;
import com.todoroo.astrid.notes.EditNoteActivity;
import com.todoroo.astrid.notes.NotesDecorationExposer;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.timers.TimerDecorationExposer;
import com.todoroo.astrid.utility.Constants;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAdapter extends CursorAdapter implements Filterable {

    public interface OnCompletedTaskListener {
        public void onCompletedTask(Task item, boolean newState);
    }

    public static final String DETAIL_SEPARATOR = " | "; //$NON-NLS-1$

    public static final String BROADCAST_EXTRA_TASK = "model"; //$NON-NLS-1$

    // --- other constants

    /** Properties that need to be read from the action item */
    public static final Property<?>[] PROPERTIES = new Property<?>[] {
        Task.ID,
        Task.TITLE,
        Task.FLAGS,
        Task.IMPORTANCE,
        Task.DUE_DATE,
        Task.COMPLETION_DATE,
        Task.HIDE_UNTIL,
        Task.DELETION_DATE,
        Task.DETAILS,
        Task.ELAPSED_SECONDS,
        Task.TIMER_START,
        Task.NOTES,
        Task.USER_ID,
        Task.USER
    };

    private static int[] IMPORTANCE_RESOURCES = new int[] {
        R.drawable.task_indicator_0,
        R.drawable.task_indicator_1,
        R.drawable.task_indicator_2,
        R.drawable.task_indicator_3,
    };

    // --- instance variables

    @Autowired
    private ExceptionService exceptionService;

    @Autowired
    private TaskService taskService;

    protected final ListFragment listFragment;
    protected final HashMap<Long, Boolean> completedItems = new HashMap<Long, Boolean>(0);
    private OnCompletedTaskListener onCompletedTaskListener = null;
    public boolean isFling = false;
    private final int resource;
    private final LayoutInflater inflater;
    private int fontSize;
    private DetailLoaderThread detailLoader;

    private final AtomicReference<String> query;

    // quick action bar
    private QuickActionWidget mBar;
    private final QuickActionListener mBarListener = new QuickActionListener();

    // measure utilities
    private final Paint paint;
    private final DisplayMetrics dm;

    // --- task detail and decoration soft caches

    public final DecorationManager decorationManager;
    public final TaskActionManager taskActionManager;

    /**
     * Constructor
     *
     * @param listFragment
     * @param resource
     *            layout resource to inflate
     * @param c
     *            database cursor
     * @param autoRequery
     *            whether cursor is automatically re-queried on changes
     * @param onCompletedTaskListener
     *            task listener. can be null
     */
    public TaskAdapter(ListFragment listFragment, int resource,
            Cursor c, AtomicReference<String> query, boolean autoRequery,
            OnCompletedTaskListener onCompletedTaskListener) {
        super(listFragment.getActivity(), c, autoRequery);
        DependencyInjectionService.getInstance().inject(this);

        inflater = (LayoutInflater) listFragment.getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        this.query = query;
        this.resource = resource;
        this.listFragment = listFragment;
        this.onCompletedTaskListener = onCompletedTaskListener;

        fontSize = Preferences.getIntegerFromString(R.string.p_fontSize, 20);
        paint = new Paint();
        dm = new DisplayMetrics();
        listFragment.getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

        detailLoader = new DetailLoaderThread();
        detailLoader.start();

        decorationManager = new DecorationManager();
        taskActionManager = new TaskActionManager();
    }

    /* ======================================================================
     * =========================================================== filterable
     * ====================================================================== */

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }

        // perform query
        TodorooCursor<Task> newCursor = taskService.fetchFiltered(
                query.get(), constraint, TaskAdapter.PROPERTIES);
        listFragment.getActivity().startManagingCursor(newCursor);
        return newCursor;
    }

    /* ======================================================================
     * =========================================================== view setup
     * ====================================================================== */

    /** Creates a new view for use in the list view */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewGroup view = (ViewGroup)inflater.inflate(resource, parent, false);

        // create view holder
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.task = new Task();
        viewHolder.view = view;
        viewHolder.rowBody = (ViewGroup)view.findViewById(R.id.rowBody);
        viewHolder.nameView = (TextView)view.findViewById(R.id.title);
        viewHolder.picture = (AsyncImageView)view.findViewById(R.id.picture);
        viewHolder.completeBox = (CheckBox)view.findViewById(R.id.completeBox);
        viewHolder.dueDate = (TextView)view.findViewById(R.id.dueDate);
        viewHolder.details1 = (TextView)view.findViewById(R.id.details1);
        viewHolder.details2 = (TextView)view.findViewById(R.id.details2);
        viewHolder.taskRow = (LinearLayout)view.findViewById(R.id.task_row);
        viewHolder.importance = (View)view.findViewById(R.id.importance);

        view.setTag(viewHolder);
        for(int i = 0; i < view.getChildCount(); i++)
            view.getChildAt(i).setTag(viewHolder);
        if(viewHolder.details1 != null)
            viewHolder.details1.setTag(viewHolder);

        // add UI component listeners
        addListeners(view);

        // populate view content
        bindView(view, context, cursor);

        return view;
    }

    /** Populates a view with content */
    @Override
    public void bindView(View view, Context context, Cursor c) {
        TodorooCursor<Task> cursor = (TodorooCursor<Task>)c;
        ViewHolder viewHolder = ((ViewHolder)view.getTag());

        Task task = viewHolder.task;
        task.clear();
        task.readFromCursor(cursor);

        setFieldContentsAndVisibility(view);
        setTaskAppearance(viewHolder, task);
    }

    /** Helper method to set the visibility based on if there's stuff inside */
    private static void setVisibility(TextView v) {
        if(v.getText().length() > 0)
            v.setVisibility(View.VISIBLE);
        else
            v.setVisibility(View.GONE);
    }

    /**
     * View Holder saves a lot of findViewById lookups.
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class ViewHolder {
        public Task task;
        public ViewGroup view;
        public ViewGroup rowBody;
        public TextView nameView;
        public CheckBox completeBox;
        public AsyncImageView picture;
        public TextView dueDate;
        public TextView details1, details2;
        public View importance;
        public LinearLayout taskRow;

        public View[] decorations;
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void setFieldContentsAndVisibility(View view) {
        Resources r = listFragment.getResources();
        ViewHolder viewHolder = (ViewHolder)view.getTag();
        Task task = viewHolder.task;

        // name
        final TextView nameView = viewHolder.nameView; {
            String nameValue = task.getValue(Task.TITLE);
            long hiddenUntil = task.getValue(Task.HIDE_UNTIL);
            if(task.getValue(Task.DELETION_DATE) > 0)
                nameValue = r.getString(R.string.TAd_deletedFormat, nameValue);
            if(hiddenUntil > DateUtilities.now())
                nameValue = r.getString(R.string.TAd_hiddenFormat, nameValue);
            nameView.setText(nameValue);
        }

        // due date / completion date
        float dueDateTextWidth = 0;
        final TextView dueDateView = viewHolder.dueDate; {
            if(!task.isCompleted() && task.hasDueDate()) {
                long dueDate = task.getValue(Task.DUE_DATE);
                long secondsLeft = dueDate - DateUtilities.now();
                if(secondsLeft > 0) {
                    dueDateView.setTextAppearance(listFragment.getActivity(), R.style.TextAppearance_TAd_ItemDueDate);
                } else {
                    dueDateView.setTextAppearance(listFragment.getActivity(), R.style.TextAppearance_TAd_ItemDueDate_Overdue);
                }

                String dateValue = formatDate(dueDate);
                dueDateView.setText(dateValue);
                dueDateTextWidth = paint.measureText(dateValue);
                setVisibility(dueDateView);
            } else if(task.isCompleted()) {
                String dateValue = DateUtilities.getDateStringWithTime(listFragment.getActivity(), new Date(task.getValue(Task.COMPLETION_DATE)));
                dueDateView.setText(r.getString(R.string.TAd_completed, dateValue));
                dueDateView.setTextAppearance(listFragment.getActivity(), R.style.TextAppearance_TAd_ItemDueDate_Completed);
                dueDateTextWidth = paint.measureText(dateValue);
                setVisibility(dueDateView);
            } else {
                dueDateView.setVisibility(View.GONE);
            }
        }

        // complete box
        final CheckBox completeBox = viewHolder.completeBox; {
            // show item as completed if it was recently checked
            if(completedItems.get(task.getId()) != null) {
                task.setValue(Task.COMPLETION_DATE,
                        completedItems.get(task.getId()) ? DateUtilities.now() : 0);
            }
            completeBox.setChecked(task.isCompleted());
            // disable checkbox if task is readonly
            completeBox.setEnabled(!viewHolder.task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY));
        }

        // image view
        final AsyncImageView pictureView = viewHolder.picture; {
            if(task.getValue(Task.USER_ID) == 0) {
                pictureView.setVisibility(View.GONE);
            } else {
                pictureView.setVisibility(View.VISIBLE);
                pictureView.setUrl(null);
                try {
                    JSONObject user = new JSONObject(task.getValue(Task.USER));
                    pictureView.setUrl(user.optString("picture")); //$NON-NLS-1$
                } catch (JSONException e) {
                    Log.w("astrid", "task-adapter-image", e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        // importance bar
        final View importanceView = viewHolder.importance; {
            int value = task.getValue(Task.IMPORTANCE);
            if(value < IMPORTANCE_RESOURCES.length)
                importanceView.setBackgroundResource(IMPORTANCE_RESOURCES[value]);
            else
                importanceView.setBackgroundResource(0);
        }

        String details;
        if(viewHolder.details1 != null) {
            if(taskDetailLoader.containsKey(task.getId()))
                details = taskDetailLoader.get(task.getId()).toString();
            else
                details = task.getValue(Task.DETAILS);
            if(TextUtils.isEmpty(details) || DETAIL_SEPARATOR.equals(details) || task.isCompleted()) {
                viewHolder.details1.setVisibility(View.GONE);
                viewHolder.details2.setVisibility(View.GONE);
            } else {
                viewHolder.details1.setVisibility(View.VISIBLE);
                while(details.startsWith(DETAIL_SEPARATOR))
                    details = details.substring(DETAIL_SEPARATOR.length());

                drawDetails(viewHolder, details, dueDateTextWidth);
            }
        }

        // details and decorations, expanded
        decorationManager.request(viewHolder);
    }

    @SuppressWarnings("nls")
    private void drawDetails(ViewHolder viewHolder, String details, float rightWidth) {
        SpannableStringBuilder prospective = new SpannableStringBuilder();
        SpannableStringBuilder actual = new SpannableStringBuilder();

        details = details.trim().replace("\n", "<br>");
        String[] splitDetails = details.split("\\|");
        viewHolder.completeBox.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        float left = viewHolder.completeBox.getMeasuredWidth() +
            ((MarginLayoutParams)viewHolder.completeBox.getLayoutParams()).leftMargin;
        int availableWidth = (int) (dm.widthPixels - left - (rightWidth + 16) * dm.density);

        int i = 0;
        for(; i < splitDetails.length; i++) {
            Spanned spanned = convertToHtml(splitDetails[i] + "  ", detailImageGetter, null);
            prospective.insert(prospective.length(), spanned);
            viewHolder.details1.setText(prospective);
            viewHolder.details1.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            if(rightWidth > 0 && viewHolder.details1.getMeasuredWidth() > availableWidth)
                break;

            actual.insert(actual.length(), spanned);
        }
        viewHolder.details1.setText(actual);
        actual.clear();

        if(i >= splitDetails.length) {
            viewHolder.details2.setVisibility(View.GONE);
            return;
        } else {
            viewHolder.details2.setVisibility(View.VISIBLE);
        }

        for(; i < splitDetails.length; i++)
            actual.insert(actual.length(), convertToHtml(splitDetails[i] + "  ", detailImageGetter, null));
        viewHolder.details2.setText(actual);
    }

    protected TaskRowListener listener = new TaskRowListener();
    /**
     * Set listeners for this view. This is called once per view when it is
     * created.
     */
    protected void addListeners(final View container) {
        ViewHolder viewHolder = (ViewHolder)container.getTag();

        // check box listener
        viewHolder.completeBox.setOnClickListener(completeBoxListener);

        // context menu listener
        container.setOnCreateContextMenuListener(listener);

        // tap listener
        container.setOnClickListener(listener);
    }

    /* ======================================================================
     * ============================================================== details
     * ====================================================================== */

    private final HashMap<String, Spanned> htmlCache = new HashMap<String, Spanned>(8);

    private Spanned convertToHtml(String string, ImageGetter imageGetter, TagHandler tagHandler) {
        if(!htmlCache.containsKey(string)) {
            Spanned html;
            try {
                html = Html.fromHtml(string, imageGetter, tagHandler);
            } catch (RuntimeException e) {
                html = Spannable.Factory.getInstance().newSpannable(string);
            }
            htmlCache.put(string, html);
            return html;
        }
        return htmlCache.get(string);
    }

    private final HashMap<Long, String> dateCache = new HashMap<Long, String>(8);

    private String formatDate(long date) {
        if(dateCache.containsKey(date))
            return dateCache.get(date);

        String string;

        if(Math.abs(date - DateUtilities.now()) < DateUtilities.ONE_DAY) {
            if(Task.hasDueTime(date))
                string = DateUtils.getRelativeTimeSpanString(listFragment.getActivity(), date, true).toString();
            else
                string = DateUtilities.getRelativeDay(listFragment.getActivity(), date).toLowerCase();
        } else {
            string = DateUtils.getRelativeDateTimeString(listFragment.getActivity(), date,
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0).toString();
            if(!Task.hasDueTime(date))
                string = string.substring(0, string.lastIndexOf(','));
        }

        dateCache.put(date, string);
        return string;
    }

    // implementation note: this map is really costly if users have
    // a large number of tasks to load, since it all goes into memory.
    // it's best to do this, though, in order to append details to each other
    private final Map<Long, StringBuilder> taskDetailLoader = Collections.synchronizedMap(new HashMap<Long, StringBuilder>(0));

    public class DetailLoaderThread extends Thread {
        @Override
        public void run() {
            // for all of the tasks returned by our cursor, verify details
            AndroidUtilities.sleepDeep(500L);
            TodorooCursor<Task> fetchCursor = taskService.fetchFiltered(
                    query.get(), null, Task.ID, Task.DETAILS, Task.DETAILS_DATE,
                    Task.MODIFICATION_DATE, Task.COMPLETION_DATE);
            try {
                Random random = new Random();

                Task task = new Task();
                for(fetchCursor.moveToFirst(); !fetchCursor.isAfterLast(); fetchCursor.moveToNext()) {
                    task.clear();
                    task.readFromCursor(fetchCursor);
                    if(task.isCompleted())
                        continue;

                    if(detailsAreRecentAndUpToDate(task)) {
                        // even if we are up to date, randomly load a fraction
                        if(random.nextFloat() < 0.1) {
                            taskDetailLoader.put(task.getId(),
                                    new StringBuilder(task.getValue(Task.DETAILS)));
                            requestNewDetails(task);
                            if(Constants.DEBUG)
                                System.err.println("Refreshing details: " + task.getId()); //$NON-NLS-1$
                        }
                        continue;
                    } else if(Constants.DEBUG) {
                        System.err.println("Forced loading of details: " + task.getId() + //$NON-NLS-1$
                        		"\n  details: " + new Date(task.getValue(Task.DETAILS_DATE)) + //$NON-NLS-1$
                        		"\n  modified: " + new Date(task.getValue(Task.MODIFICATION_DATE))); //$NON-NLS-1$
                    }
                    addTaskToLoadingArray(task);

                    task.setValue(Task.DETAILS, DETAIL_SEPARATOR);
                    task.setValue(Task.DETAILS_DATE, DateUtilities.now());
                    taskService.save(task);

                    requestNewDetails(task);
                }
            } catch (Exception e) {
                // suppress silently
            } finally {
                fetchCursor.close();
            }
        }

        private boolean detailsAreRecentAndUpToDate(Task task) {
            return task.getValue(Task.DETAILS_DATE) >= task.getValue(Task.MODIFICATION_DATE) &&
                !TextUtils.isEmpty(task.getValue(Task.DETAILS));
        }

        private void addTaskToLoadingArray(Task task) {
            StringBuilder detailStringBuilder = new StringBuilder();
            taskDetailLoader.put(task.getId(), detailStringBuilder);
        }

        private void requestNewDetails(Task task) {
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            listFragment.getActivity().sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        }
    }

    /**
     * Add detail to a task
     *
     * @param id
     * @param detail
     */
    public void addDetails(long id, String detail) {
        final StringBuilder details = taskDetailLoader.get(id);
        if(details == null)
            return;
        synchronized(details) {
            if(details.toString().contains(detail))
                return;
            if(details.length() > 0)
                details.append(DETAIL_SEPARATOR);
            details.append(detail);
            Task task = new Task();
            task.setId(id);
            task.setValue(Task.DETAILS, details.toString());
            task.setValue(Task.DETAILS_DATE, DateUtilities.now());
            taskService.save(task);
        }

        listFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    private final ImageGetter detailImageGetter = new ImageGetter() {
        private final HashMap<Integer, Drawable> cache =
            new HashMap<Integer, Drawable>(3);
        @SuppressWarnings("nls")
        public Drawable getDrawable(String source) {
            Resources r = listFragment.getResources();

            if(source.equals("silk_clock"))
                source = "details_alarm";
            else if(source.equals("silk_tag_pink"))
                source = "details_tag";
            else if(source.equals("silk_date"))
                source = "details_repeat";
            else if(source.equals("silk_note"))
                source = "details_note";

            int drawable = r.getIdentifier("drawable/" + source, null, Constants.PACKAGE);
            if(drawable == 0)
                return null;
            Drawable d;
            if(!cache.containsKey(drawable)) {
                 d = r.getDrawable(drawable);
                 d.setBounds(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
                cache.put(drawable, d);
            } else
                d = cache.get(drawable);
            return d;
        }
    };

    /* ======================================================================
     * ============================================================== add-ons
     * ====================================================================== */

    /**
     * Called to tell the cache to be cleared
     */
    public void flushCaches() {
        completedItems.clear();
        decorationManager.clearCache();
        taskActionManager.clearCache();
        taskDetailLoader.clear();
        detailLoader = new DetailLoaderThread();
        detailLoader.start();
    }

    /**
     * Called to tell the cache to be cleared
     */
    public void flushSpecific(long taskId) {
        completedItems.put(taskId, null);
        decorationManager.clearCache(taskId);
        taskActionManager.clearCache(taskId);
        taskDetailLoader.remove(taskId);
    }

    /**
     * AddOnManager for TaskDecorations
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class DecorationManager extends TaskAdapterAddOnManager<TaskDecoration> {

        public DecorationManager() {
            super(listFragment.getActivity());
        }

        private final TaskDecorationExposer[] exposers = new TaskDecorationExposer[] {
                new TimerDecorationExposer(),
                new NotesDecorationExposer()
        };

        /**
         * Request add-ons for the given task
         * @return true if cache miss, false if cache hit
         */
        @Override
        public boolean request(ViewHolder viewHolder) {
            long taskId = viewHolder.task.getId();

            Collection<TaskDecoration> list = initialize(taskId);
            if(list != null) {
                draw(viewHolder, taskId, list);
                return false;
            }

            // request details
            draw(viewHolder, taskId, get(taskId));

            for(TaskDecorationExposer exposer : exposers) {
                TaskDecoration deco = exposer.expose(viewHolder.task);
                if(deco != null) {
                    addNew(viewHolder.task.getId(), exposer.getAddon(), deco, viewHolder);
                }
            }

            return true;
        }

        @Override
        protected void draw(ViewHolder viewHolder, long taskId, Collection<TaskDecoration> decorations) {
            if(decorations == null || viewHolder.task.getId() != taskId)
                return;

            reset(viewHolder, taskId);
            if(decorations.size() == 0)
                return;


            int i = 0;
            boolean colorSet = false;
            if(viewHolder.decorations == null || viewHolder.decorations.length != decorations.size())
                viewHolder.decorations = new View[decorations.size()];
            for(TaskDecoration decoration : decorations) {
                if(decoration.color != 0 && !colorSet) {
                    colorSet = true;
                    viewHolder.view.setBackgroundColor(decoration.color);
                }
                if(decoration.decoration != null) {
                    View view = decoration.decoration.apply(listFragment.getActivity(), viewHolder.taskRow);
                    viewHolder.decorations[i] = view;
                    switch(decoration.position) {
                    case TaskDecoration.POSITION_LEFT: {
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                        params.addRule(RelativeLayout.BELOW, R.id.completeBox);
                        view.setLayoutParams(params);
                        viewHolder.rowBody.addView(view);
                        break;
                    }
                    case TaskDecoration.POSITION_RIGHT:
                        viewHolder.taskRow.addView(view, viewHolder.taskRow.getChildCount());
                    }
                }
                i++;
            }
        }

        @Override
        protected void reset(ViewHolder viewHolder, long taskId) {
            if(viewHolder.decorations != null) {
                for(View view : viewHolder.decorations) {
                    viewHolder.rowBody.removeView(view);
                    viewHolder.taskRow.removeView(view);
                }
                viewHolder.decorations = null;
            }
            viewHolder.view.setBackgroundResource(android.R.drawable.list_selector_background);
        }

        @Override
        protected Intent createBroadcastIntent(Task task) {
            return null;
        }
    }

    /**
     * AddOnManager for TaskActions
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class TaskActionManager extends TaskAdapterAddOnManager<TaskAction> {

        private final Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_ACTIONS);

        public TaskActionManager() {
            super(listFragment.getActivity());
        }

        @Override
        protected Intent createBroadcastIntent(Task task) {
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            return broadcastIntent;
        }

        @Override
        public synchronized void addNew(long taskId, String addOn, final TaskAction item, ViewHolder thisViewHolder) {
            addIfNotExists(taskId, addOn, item);
            if(mBar != null) {
                if (listFragment instanceof ListFragment) {
                    ListView listView = ((ListFragment) listFragment).getListView();
                    ViewHolder myHolder = null;

                    // update view if it is visible
                    int length = listView.getChildCount();
                    for(int i = 0; i < length; i++) {
                        ViewHolder viewHolder = (ViewHolder) listView.getChildAt(i).getTag();
                        if(viewHolder == null || viewHolder.task.getId() != taskId)
                            continue;
                        myHolder = viewHolder;
                        break;
                    }

                    if(myHolder != null) {
                        final ViewHolder viewHolder = myHolder;
                        listFragment.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBarListener.addWithAction(item);
                                if (!viewHolder.task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY))
                                    mBar.show(viewHolder.view);
                            }
                        });
                    }
                }
            }
        }

        @Override
        public Collection<TaskAction> get(long taskId) {
            return super.get(taskId);
        }

        @Override
        protected void draw(final ViewHolder viewHolder, final long taskId, Collection<TaskAction> actions) {
            // do not draw
        }

        @Override
        protected void reset(ViewHolder viewHolder, long taskId) {
            // do not draw
        }
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        fontSize = Preferences.getIntegerFromString(R.string.p_fontSize, 20);

    }

    protected final View.OnClickListener completeBoxListener = new View.OnClickListener() {
        public void onClick(View v) {
            ViewHolder viewHolder = (ViewHolder)((View)v.getParent().getParent()).getTag();
            Task task = viewHolder.task;

            completeTask(task, ((CheckBox)v).isChecked());

            // set check box to actual action item state
            setTaskAppearance(viewHolder, task);
        }
    };

    private final class QuickActionListener implements OnQuickActionClickListener {
        private final HashMap<Integer, TaskAction> positionActionMap =
            new HashMap<Integer, TaskAction>(2);
        private long taskId;
        private int itemCount = 0;

        public void initialize(long newTaskId) {
            this.taskId = newTaskId;
            itemCount = 0;
            positionActionMap.clear();
            mBar.setOnQuickActionClickListener(this);
        }

        public void addWithAction(TaskAction item) {
            Drawable drawable;
            if(item.drawable > 0)
                drawable = listFragment.getResources().getDrawable(item.drawable);
            else
                drawable = new BitmapDrawable(listFragment.getResources(), item.icon);
            addWithAction(new QuickAction(drawable, item.text), item);
        }

        public void addWithAction(QuickAction quickAction, TaskAction taskAction) {
            positionActionMap.put(itemCount++, taskAction);
            mBar.addQuickAction(quickAction);
        }

        public void onQuickActionClicked(QuickActionWidget widget, int position){
            if(mBar != null)
                mBar.dismiss();
            mBar = null;

            if(position == 0) {
                TaskEditFragment taskEditFragment = (TaskEditFragment) listFragment.getFragmentManager()
                .findFragmentById(R.id.taskedit_fragment);

                Intent intent = new Intent(listFragment.getActivity(), TaskEditActivity.class);
                intent.putExtra(TaskEditFragment.TOKEN_ID, taskId);
                if (taskEditFragment == null || !taskEditFragment.isInLayout()) {
                    listFragment.startActivityForResult(intent, TaskListFragment.ACTIVITY_EDIT_TASK);
                } else {
                    taskEditFragment.populateFields(intent, true);
                }
            } else {
                flushSpecific(taskId);
                try {
                    TaskAction taskAction = positionActionMap.get(position);
                    if(taskAction != null) {
                        taskAction.intent.send();
                    }
                } catch (Exception e) {
                    exceptionService.displayAndReportError(listFragment.getActivity(),
                            "Error launching action", e); //$NON-NLS-1$
                }
            }
            notifyDataSetChanged();
        }
    }

    private class TaskRowListener implements OnCreateContextMenuListener, OnClickListener {

        // prepare quick action bar
        private void prepareQuickActionBar(ViewHolder viewHolder, Collection<TaskAction> collection){
            mBar = new QuickActionBar(viewHolder.view.getContext());
            QuickAction editAction = new QuickAction(listFragment.getActivity(), R.drawable.ic_qbar_edit,
                    listFragment.getString(R.string.TAd_actionEditTask));
            mBarListener.initialize(viewHolder.task.getId());
            mBarListener.addWithAction(editAction, null);

            if(collection != null) {
                for(TaskAction item : collection) {
                    mBarListener.addWithAction(item);
                }
            }


        }

        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            // this is all a big sham. it's actually handled in Task List
            // Activity. however, we need this to be here.
        }

        @Override
        public void onClick(View v) {
            // expand view (unless deleted)
            final ViewHolder viewHolder = (ViewHolder)v.getTag();
            if(viewHolder.task.isDeleted())
                return;

            long taskId = viewHolder.task.getId();

            if(isIntroTask(viewHolder.task)) {
                Intent intent = new Intent(ContextManager.getContext(), EditNoteActivity.class);
                intent.putExtra(EditNoteActivity.EXTRA_TASK_ID, viewHolder.task.getId());
                listFragment.startActivity(intent);
                return;
            }

            Collection<TaskAction> actions = taskActionManager.get(taskId);
            prepareQuickActionBar(viewHolder, actions);
            //mBarAnchor = v;
            if(actions != null && !viewHolder.task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY))
                mBar.show(v);
            taskActionManager.request(viewHolder);


            notifyDataSetChanged();
        }

        private boolean isIntroTask(Task task) {
            if(task.getId() <= 3)
                return true;
            return false;
        }
    }

    /**
     * Call me when the parent presses trackpad
     */
    public void onTrackpadPressed(View container) {
        if(container == null)
            return;

        final CheckBox completeBox = ((CheckBox)container.findViewById(R.id.completeBox));
        completeBox.performClick();
    }

    /** Helper method to adjust a tasks' appearance if the task is completed or
     * uncompleted.
     *
     * @param actionItem
     * @param name
     * @param progress
     */
    void setTaskAppearance(ViewHolder viewHolder, Task task) {
        boolean state = task.isCompleted();

        viewHolder.completeBox.setChecked(state);
        viewHolder.completeBox.setEnabled(!viewHolder.task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY));

        TextView name = viewHolder.nameView;
        if(state) {
            name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextAppearance(listFragment.getActivity(), R.style.TextAppearance_TAd_ItemTitle_Completed);
        } else {
            name.setPaintFlags(name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextAppearance(listFragment.getActivity(), R.style.TextAppearance_TAd_ItemTitle);
        }
        name.setTextSize(fontSize);
        float detailTextSize = Math.max(10, fontSize * 12 / 20);
        if(viewHolder.details1 != null)
            viewHolder.details1.setTextSize(detailTextSize);
        if(viewHolder.details2 != null)
            viewHolder.details2.setTextSize(detailTextSize);
        if(viewHolder.dueDate != null)
            viewHolder.dueDate.setTextSize(detailTextSize);
        paint.setTextSize(detailTextSize);
    }

    /**
     * This method is called when user completes a task via check box or other
     * means
     *
     * @param container
     *            container for the action item
     * @param newState
     *            state that this task should be set to
     * @param completeBox
     *            the box that was clicked. can be null
     */
    protected void completeTask(final Task task, final boolean newState) {
        if(task == null)
            return;

        if (newState != task.isCompleted()) {
            completedItems.put(task.getId(), newState);
            taskService.setComplete(task, newState);

            if(onCompletedTaskListener != null)
                onCompletedTaskListener.onCompletedTask(task, newState);
        }
    }

}
