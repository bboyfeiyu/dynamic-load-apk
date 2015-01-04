
package com.ryg.dynamicload.sample.mainhost;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ryg.dynamicload.internal.DLIntent;
import com.ryg.dynamicload.internal.DLPluginManager;
import com.ryg.utils.DLUtils;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity implements OnItemClickListener {
    private static final String TAG = "MainActivity";

    public static final String FROM = "extra.from";
    public static final int FROM_INTERNAL = 0;
    public static final int FROM_EXTERNAL = 1;

    private ArrayList<PluginItem> mPluginItems = new ArrayList<PluginItem>();
    private PluginAdapter mPluginAdapter;

    private ListView mListView;
    private TextView mNoPluginTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initView() {
        mPluginAdapter = new PluginAdapter();
        mListView = (ListView) findViewById(R.id.plugin_list);
        mNoPluginTextView = (TextView) findViewById(R.id.no_plugin);
    }

    private void initData() {
        String pluginFolder = Environment.getExternalStorageDirectory() + "/DynamicLoadHost";
        File file = new File(pluginFolder);
        File[] plugins = file.listFiles();
        if (plugins == null || plugins.length == 0) {
            mNoPluginTextView.setVisibility(View.VISIBLE);
            return;
        }

        for (File plugin : plugins) {
            PluginItem item = new PluginItem();
            item.pluginPath = plugin.getAbsolutePath();
            item.packageInfo = DLUtils.getPackageInfo(this, item.pluginPath);
            if (item.packageInfo.activities != null && item.packageInfo.activities.length > 0) {
                item.launcherActivityName = item.packageInfo.activities[0].name;
            }
            mPluginItems.add(item);
        }

        mListView.setAdapter(mPluginAdapter);
        mListView.setOnItemClickListener(this);
        mPluginAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                DLUtils.showDialog(this, getString(R.string.action_about),
                        getString(R.string.introducation));
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class PluginAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public PluginAdapter() {
            mInflater = MainActivity.this.getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mPluginItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mPluginItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.plugin_item, parent, false);
                holder = new ViewHolder();
                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                holder.apkName = (TextView) convertView.findViewById(R.id.apk_name);
                holder.packageName = (TextView) convertView.findViewById(R.id.package_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            PluginItem item = mPluginItems.get(position);
            PackageInfo packageInfo = item.packageInfo;
            holder.appIcon.setImageDrawable(DLUtils.getAppIcon(MainActivity.this, item.pluginPath));
            holder.appName.setText(DLUtils.getAppLabel(MainActivity.this, item.pluginPath));
            holder.apkName.setText(item.pluginPath.substring(item.pluginPath
                    .lastIndexOf(File.separatorChar) + 1));
            holder.packageName.setText(packageInfo.applicationInfo.packageName);
            return convertView;
        }
    }

    private static class ViewHolder {
        public ImageView appIcon;
        public TextView appName;
        public TextView apkName;
        public TextView packageName;
    }

    public static class PluginItem {
        public PackageInfo packageInfo;
        public String pluginPath;
        public String launcherActivityName;

        public PluginItem() {
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PluginItem item = mPluginItems.get(position);
        DLPluginManager pluginManager = DLPluginManager.getInstance(this);
        if (!pluginManager.verifyPlugin(item.pluginPath, item.packageInfo)) {
            Log.e(TAG, "verify plugin failed: " + item.pluginPath);
            return;
        }

        DLIntent dlIntent = new DLIntent(item.packageInfo.packageName,
                item.launcherActivityName);
        dlIntent.setDexPath(item.pluginPath);
        //
        // dlIntent.setPathClassLoader(Person.class.getClassLoader());
        dlIntent.putExtra("person", new Person("simple", 24));
        dlIntent.putExtra("key", "hello");
        //
        Log.d(TAG, "启动插件 --- ");

        pluginManager.launchPluginActivity(dlIntent);
    }

    /**
     * @author mrsimple
     */
    public static class Person implements Parcelable {

        String mName;
        int mAge;

        public Person(String name, int age) {
            mName = name;
            mAge = age;
        }

        protected Person(Parcel in) {
            mName = in.readString();
            mAge = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mName);
            dest.writeInt(mAge);
        }

        public static final Parcelable.Creator<Person> CREATOR = new Creator<Person>() {

            @Override
            public Person createFromParcel(Parcel source) {
                return new Person(source);
            }

            @Override
            public Person[] newArray(int size) {
                return new Person[size];
            }

        };

        @Override
        public String toString() {
            return "Person [mName=" + mName + ", mAge=" + mAge + "]";
        }

    }

}
