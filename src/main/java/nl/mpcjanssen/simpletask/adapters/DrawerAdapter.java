package nl.mpcjanssen.simpletask.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import nl.mpcjanssen.simpletask.R;

import java.util.ArrayList;
import java.util.List;

public class DrawerAdapter extends BaseAdapter implements ListAdapter {

    ArrayList<Container> items;
    int contextHeaderPos;
    int projectHeaderPos;
    private LayoutInflater m_inflater;

    public DrawerAdapter(LayoutInflater inflater,
			 String contextHeader,
			 List<String> contexts,
			 String projectHeader,
			 List<String> projects) {
        this.m_inflater = inflater;
<<<<<<< HEAD
        this.items = new ArrayList<>();
        this.items.add(contextHeader);
=======
        this.items = new ArrayList<Container>();
	Container c = new Container();
	c.type = Container.HEADER;
        c.value = contextHeader;
	this.items.add(c);
>>>>>>> origin/issue160
        contextHeaderPos = 0;
	c = new Container();
	c.type = Container.OR_SWITCH;
        c.value = "";
	this.items.add(c);
	for (String s : contexts) {
	    c = new Container();
	    c.type = Container.ITEM;
	    c.value = s;
	    this.items.add(c);
	}
	c = new Container();
	c.type = Container.HEADER;
        c.value = projectHeader;
	this.items.add(c);
	projectHeaderPos = items.size();
	for (String s : projects) {
	    c = new Container();
	    c.type = Container.ITEM;
	    c.value = s;
	    this.items.add(c);
	}
    }

    private boolean isHeader(int position) {
        return (items.get(position).type == Container.HEADER);
    }
    
    private boolean isOrSwitch(int position) {
        return (items.get(position).type == Container.OR_SWITCH);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public String getItem(int position) {
        return items.get(position).value;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true; // To change body of implemented methods use File |
        // Settings | File Templates.
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv;
        if (isHeader(position)) {
            convertView = m_inflater.inflate(R.layout.drawer_list_header, parent, false);
            tv = (TextView) convertView;
            ListView lv = (ListView) parent;
            if (lv.isItemChecked(position)) {
                tv.setText(items.get(position).value + " inverted");
            } else {
                tv.setText(items.get(position).value);
            }

        } else if (isOrSwitch(position)) {
           convertView = m_inflater.inflate(R.layout.drawer_list_or_switch, parent, false);
            ListView lv = (ListView) parent;
	    LinearLayout layout = (LinearLayout) convertView;
	    Switch s = (Switch)layout.findViewById(R.id.switchView);
	    s.setChecked(lv.isItemChecked(position));
	} else {
            if (convertView == null) {
                convertView = m_inflater.inflate(R.layout.drawer_list_item_checked, parent, false);
            }
            tv = (TextView) convertView;
            tv.setText(items.get(position).value.substring(1));
        }
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
	return items.get(position).type;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
            return true;
    }

    public int getIndexOf(String item) {
        return items.indexOf(item);
    }

    public int getContextHeaderPosition () {
        return contextHeaderPos;
    }

    public int getProjectsHeaderPosition () {
        return projectHeaderPos;
    }

    private class Container {
	static final int HEADER = 0;
	static final int NOT_SWITCH = 1;
	static final int OR_SWITCH = 2;
	static final int ITEM = 3;
	public int type;
	public String value; 
    }

 }
