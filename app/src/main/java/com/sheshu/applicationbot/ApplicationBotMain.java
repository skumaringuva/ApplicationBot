package com.sheshu.applicationbot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ApplicationBotMain extends AppCompatActivity {
    private static final String TAG = "appBot";
    List<PInfo> mPackages;
    Spinner mActivitySpinner;
    Spinner mPackageSpinner;
    Spinner mContentProviderSpinner;
    Spinner mServicesSpinner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_bot_main);
        mActivitySpinner = (Spinner) findViewById(R.id.activity_spinner);
        mPackageSpinner = (Spinner) findViewById(R.id.package_spinner);
        mServicesSpinner = (Spinner) findViewById(R.id.service_spinner);
        mContentProviderSpinner = (Spinner) findViewById(R.id.content_resolve_spinner);
        mPackages = getPackages();
        PackageManager pManager = getPackageManager();
        for (PInfo pInfo : mPackages) {
            Intent intent = new Intent();
            intent.setPackage(pInfo.pname);
            //activities holds the activities defined in the package
            try {
                pInfo.mActivities = pManager.queryIntentActivities(intent, 0);
                for (ResolveInfo activity : pInfo.mActivities) {
                    Log.e(TAG, "Package: " + pInfo.pname + " Activity: " + ((ActivityInfo) activity.activityInfo).name);
                }
                pInfo.mServices = pManager.queryIntentServices(intent, 0);
                for (ResolveInfo service : pInfo.mServices) {
                    Log.e(TAG, "Package: " + pInfo.pname + " Service: " + ((ServiceInfo) service.serviceInfo).name);
                }
                pInfo.mContentProviders = pManager.queryContentProviders(pInfo.mApplicationInfo.processName,
                        pInfo.mApplicationInfo.uid, 0);
                for (ProviderInfo provider : pInfo.mContentProviders) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        Log.e(TAG, "Package: " + pInfo.pname + " provider: " + ((ProviderInfo) provider).name);
                        //     Context appContext = this.createPackageContext(pInfo.pname,0);
                        //     Log.e(TAG, "appContext: " + pInfo.pname + " appContext: " + appContext.getPackageName()+" "+appContext.getPackageName());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Package name: " + pInfo.pname);
            }
        }
        mPackageSpinner.setAdapter(new PackageListAdapter(this));
        mPackageSpinner.setSelection(0);
        mPackageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((ActivityListAdapter) mActivitySpinner.getAdapter()).updateIndex(position);
                ((ServiceListAdapter) mServicesSpinner.getAdapter()).updateIndex(position);
                ((ContentProiderAdapter) mContentProviderSpinner.getAdapter()).updateIndex(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mActivitySpinner.setAdapter(new ActivityListAdapter(this, 0));
        mServicesSpinner.setAdapter(new ServiceListAdapter(this, 0));
        mContentProviderSpinner.setAdapter(new ContentProiderAdapter(this, 0));
        createButton(R.id.launch_activity_result);
        createButton(R.id.launch_activity);
        createButton(R.id.launch_service);
        createButton(R.id.query_content);
    }

    private void createButton(int id){
        Button launchButton = (Button) findViewById(id);
        launchButton.setOnClickListener(mButtonListener);

    }

    private View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean isForResult = false;
            boolean isService = false;
            if (v.getId() == R.id.launch_activity_result)
                isForResult = true;

            if(v.getId() == R.id.launch_service ){
                isService = true;
            }


            if (v.getId() == R.id.launch_activity || v.getId() == R.id.launch_activity_result ||v.getId() == R.id.launch_service) {
                String packageName = mPackages.get(mPackageSpinner.getSelectedItemPosition()).pname;
                String className ;

                if(isService){
                    className = mPackages.get(mPackageSpinner.getSelectedItemPosition())
                            .mServices.get(mServicesSpinner.getSelectedItemPosition()).serviceInfo.name;
                }
                else {
                   className = mPackages.get(mPackageSpinner.getSelectedItemPosition())
                            .mActivities.get(mActivitySpinner.getSelectedItemPosition()).activityInfo.name;
                }
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(packageName, className));
                    if (isForResult)
                        startActivityForResult(intent, 0);
                    else if(isService)
                        startService(intent);
                    else
                        startActivity(intent);
                } catch (Exception e) {
                    dumpLog(e);
                }
            } // end if

            else if(v.getId() == R.id.query_content ){

                if(mContentProviderSpinner.getSelectedItemPosition()>=0) {
                    String authority = mPackages.get(mPackageSpinner.getSelectedItemPosition())
                            .mContentProviders.get(mContentProviderSpinner.getSelectedItemPosition()).authority;
                    queryAuthority(authority);
                }
            }




        }// end onclick
    };


    private void dumpLog(Exception e){
        final TextView log = (TextView) findViewById(R.id.log_text);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        log.setText(sw.toString());
        Log.e(TAG,sw.toString());

    }


    private void queryAuthority(String authority){

        Uri uri = Uri.parse( "content://"+authority);
        String[] projection = new String[]{};
        String selection = "";
        String[] selectionArgs = null;
        String sortOrder = null;


        try{
            Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs,
                    sortOrder);
            dumpCursor(cursor);
            cursor.close();
        }
        catch (Exception e){
            dumpLog(e);
        }
    }


    private void getAppActivities() {
        PackageManager pManager = getPackageManager();
        Intent startIntent = getIntent();
        List<ResolveInfo> apps = pManager.queryIntentActivities(startIntent, 0);
        int count = apps.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = apps.get(i);
            String packageName = info.activityInfo.packageName;
            Intent intent = new Intent();
            intent.setPackage(packageName);
            //activities holds the activities defined in the package
            List<ResolveInfo> activities = pManager.queryIntentActivities(intent, 0);
        }
    }
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("ApplicationBotMain Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }
    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public void onStop() {
        super.onStop();
    }
    class PInfo implements ItemInterface {
        private String appname = "";
        private String pname = "";
        private String classname = "";
        private int versionCode = 0;
        private Drawable icon;
        List<ResolveInfo> mActivities;
        List<ResolveInfo> mServices;
        List<ProviderInfo> mContentProviders;
        ApplicationInfo mApplicationInfo;
        private void prettyPrint() {
            Log.e("Applist ", appname + "   " + pname + "   " + classname + "   " + versionCode);
        }
        @Override
        public Object getItemWithId(int id) {
            if (id == 101)
                return appname;
            if (id == 102)
                return pname;
            if (id == 103)
                return classname;
            return null;
        }
    }
    public ArrayList<PInfo> getPackages() {
        Log.e("AppNo", "Inside Packages");
        ArrayList<PInfo> apps = getInstalledApps(false); /* false = no system packages */
        Collections.sort(apps, new InfoCompare());
        final int max = apps.size();
        Log.e("AppNo", "no of apps " + max);
        for (int i = 0; i < max; i++) {
            apps.get(i).prettyPrint();
        }
        return apps;
    }
    public ArrayList<PInfo> getInstalledApps(boolean getSysPackages) {
        ArrayList<PInfo> res = new ArrayList<PInfo>();
        List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            if ((!getSysPackages) && (p.versionName == null)) {
                continue;
            }
            PInfo newInfo = new PInfo();
            newInfo.appname = p.applicationInfo.loadLabel(getPackageManager()).toString();
            newInfo.pname = p.packageName;
            newInfo.classname = p.applicationInfo.className;
            newInfo.versionCode = p.versionCode;
            newInfo.icon = p.applicationInfo.loadIcon(getPackageManager());
            newInfo.mApplicationInfo = p.applicationInfo;
            // newInfo.mContentProviders  = p.providers;
            Intent app = getPackageManager().getLaunchIntentForPackage(p.packageName);
            if (app != null) {
                //dleteintent(app, newInfo.appname, newInfo.icon);
                res.add(newInfo);
            }
        }
        return res;
    }
    class InfoCompare implements Comparator<PInfo> {
        @Override
        public int compare(PInfo o1, PInfo o2) {
            return o1.appname.compareTo(o2.appname);
        }
    }

    class PackageListAdapter extends BaseAdapter {
        final Activity mActivity;
        private final LayoutInflater mInflater;
        PackageListAdapter(Activity activity) {
            mActivity = activity;
            mInflater = activity.getLayoutInflater();
        }
        @Override
        public int getCount() {
            return mPackages != null ? mPackages.size() : 0;
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
            if (convertView == null)
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
            TextView tv1 = (TextView) convertView.findViewById(android.R.id.text1);
            tv1.setText(mPackages.get(position).appname);
            return convertView;
        }
    }

    class ActivityListAdapter extends BaseAdapter {
        final Activity mActivity;
        private final LayoutInflater mInflater;
        PInfo mInfo;
        ActivityListAdapter(Activity activity, int appItemIndex) {
            mActivity = activity;
            mInflater = activity.getLayoutInflater();
            mInfo = mPackages.get(appItemIndex);
        }
        @Override
        public int getCount() {
            return mInfo.mActivities != null ? mInfo.mActivities.size() : 0;
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
            if (convertView == null)
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
            TextView tv1 = (TextView) convertView.findViewById(android.R.id.text1);
            tv1.setText((mInfo.mActivities.get(position).activityInfo).name);
            return convertView;
        }
        public void updateIndex(int position) {
            mInfo = mPackages.get(position);
            notifyDataSetChanged();
        }
    }

    class ServiceListAdapter extends BaseAdapter {
        final Activity mActivity;
        private final LayoutInflater mInflater;
        PInfo mInfo;
        ServiceListAdapter(Activity activity, int appItemIndex) {
            mActivity = activity;
            mInflater = activity.getLayoutInflater();
            mInfo = mPackages.get(appItemIndex);
        }
        @Override
        public int getCount() {
            return mInfo.mServices != null ? mInfo.mServices.size() : 0;
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
            if (convertView == null)
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
            TextView tv1 = (TextView) convertView.findViewById(android.R.id.text1);
            tv1.setText((mInfo.mServices.get(position).serviceInfo).name);
            return convertView;
        }
        public void updateIndex(int position) {
            mInfo = mPackages.get(position);
            notifyDataSetChanged();
        }
    }

    class ContentProiderAdapter extends BaseAdapter {
        final Activity mActivity;
        private final LayoutInflater mInflater;
        PInfo mInfo;
        ContentProiderAdapter(Activity activity, int appItemIndex) {
            mActivity = activity;
            mInflater = activity.getLayoutInflater();
            mInfo = mPackages.get(appItemIndex);
        }
        @Override
        public int getCount() {
            return mInfo.mContentProviders != null ? mInfo.mContentProviders.size() : 0;
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
            if (convertView == null)
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
            TextView tv1 = (TextView) convertView.findViewById(android.R.id.text1);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                tv1.setText(mInfo.mContentProviders.get(position).authority);
            }
            return convertView;
        }
        public void updateIndex(int position) {
            mInfo = mPackages.get(position);
            notifyDataSetChanged();
        }
    }

    //https://stackoverflow.com/questions/3105080/output-values-found-in-cursor-to-logcat-android/13106260
    private void dumpCursor(Cursor cursor){
        if (cursor.moveToFirst()) {
            do {
                StringBuilder sb = new StringBuilder();
                int columnsQty = cursor.getColumnCount();
                for (int idx=0; idx<columnsQty; ++idx) {
                    sb.append(cursor.getString(idx));
                    if (idx < columnsQty - 1)
                        sb.append("; ");
                }
                Log.v(TAG, String.format("Row: %d, Values: %s", cursor.getPosition(),
                        sb.toString()));
            } while (cursor.moveToNext());
        }

    }
}
