package com.sheshu.applicationbot;

import android.app.Activity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by SheshuKumar on 10/2/2016.
 */

public class GenericListAdapter extends BaseAdapter {

    private final Activity mActivity;
    private final LayoutInflater mInflater;
    private List<Object> mItemList;


    List<Pair<Integer,Integer>> mMapping = new ArrayList<>();

    GenericListAdapter(Activity activity, List<Object> itemList,List<Pair<Integer,Integer>> mapping)
    {
        mActivity = activity;
        mInflater = activity.getLayoutInflater();
        mItemList = itemList;
        mMapping = mapping;
    }
    @Override
    public int getCount() {
        return (mItemList!=null?mItemList.size():0);
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView!=null){
            convertView = mInflater.inflate(mMapping.get(0).first,null);
        }
        for(int i=1;i<mMapping.size();i++ ){
            Pair<Integer,Integer> item = mMapping.get(i);
            TextView v =(TextView) convertView.findViewById(item.first);
            v.setText((String)((ItemInterface)mItemList.get(position)).getItemWithId(item.second));
        }

        return convertView;
    }
}
