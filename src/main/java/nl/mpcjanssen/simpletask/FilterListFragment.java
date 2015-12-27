package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.os.Bundle;
<<<<<<< HEAD
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
=======
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
>>>>>>> origin/issue160

import java.util.ArrayList;

public class FilterListFragment extends Fragment {

    final static String TAG = FilterListFragment.class.getSimpleName();
    private ListView lv;
    private Switch invertSwitch;
    private Switch orSwitch;
    private GestureDetector gestureDetector;
    @Nullable
    ActionBar actionbar;
    String type;
    private ArrayList<String> selectedItems;
    private boolean not;
<<<<<<< HEAD
    private Logger log;

=======
    private boolean isOr;
    
>>>>>>> origin/issue160
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = LoggerFactory.getLogger(this.getClass());
        log.debug("onCreate() this:" + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.debug("onDestroy() this:" + this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        log.debug("onSaveInstanceState() this:" + this);
        outState.putStringArrayList("selectedItems", getSelectedItems());
        outState.putBoolean("not", getNot());
        outState.putBoolean("isOr", getOr());

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        log.debug("onCreateView() this:" + this + " savedInstance:" + savedInstanceState);

        Bundle arguments = getArguments();
        ArrayList<String> items = arguments.getStringArrayList(FilterActivity.FILTER_ITEMS);
        actionbar = getActivity().getActionBar();

        if (savedInstanceState != null) {
            selectedItems = savedInstanceState.getStringArrayList("selectedItems");
            not = savedInstanceState.getBoolean("not");
	    isOr = savedInstanceState.getBoolean("isOr");
        } else {
            selectedItems = arguments.getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS);
            not = arguments.getBoolean(FilterActivity.INITIAL_NOT);
            isOr = arguments.getBoolean(FilterActivity.INITIAL_OR);	    
        }

        log.debug("Fragment bundle:" + this + " arguments:" + arguments);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.multi_filter,
                container, false);

        invertSwitch = (Switch) layout.findViewById(R.id.invertSwitch);
        orSwitch = (Switch) layout.findViewById(R.id.orSwitch);

        lv = (ListView) layout.findViewById(R.id.listview);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        lv.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.simple_list_item_multiple_choice, items));

        for (int i = 0; i < items.size(); i++) {
            if (selectedItems!=null && selectedItems.contains(items.get(i))) {
                lv.setItemChecked(i, true);
            }
        }

<<<<<<< HEAD
        cb.setChecked(not);
<<<<<<< HEAD
=======
=======
        invertSwitch.setChecked(not);
	orSwitch.setChecked(isOr);
>>>>>>> origin/issue160

        gestureDetector = new GestureDetector(SimpletaskApplication.getAppContext(),
                new FilterGestureDetector());
        OnTouchListener gestureListener = new OnTouchListener() {
            @Override
            public boolean onTouch(@NotNull View v, @NotNull MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    v.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                    return true;
                }
                return false;
            }
        };

        lv.setOnTouchListener(gestureListener);
>>>>>>> origin/macroid
        return layout;
    }

    public boolean getNot() {
<<<<<<< HEAD
        if (selectedItems == null) {
            // Tab was not displayed so no selections were changed
            return  getArguments().getBoolean(FilterActivity.INITIAL_NOT);
=======
        if (invertSwitch == null) {
            return not;
>>>>>>> origin/issue160
        } else {
            return invertSwitch.isChecked();
        }
    }

    public boolean getOr() {
        if (orSwitch == null) {
            return isOr;
        } else {
            return orSwitch.isChecked();
        }
    }

    public ArrayList<String> getSelectedItems() {

        ArrayList<String> arr = new ArrayList<>();
        if (selectedItems == null) {
            // Tab was not displayed so no selections were changed
            return getArguments().getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS);
        }
        int size = lv.getCount();
        for (int i = 0; i < size; i++) {
            if (lv.isItemChecked(i)) {
                arr.add((String) lv.getAdapter().getItem(i));
            }
        }
        return arr;
    }
}
