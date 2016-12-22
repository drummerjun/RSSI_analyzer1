package com.empers.rssi_analyzer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;

import com.empers.rssi_analyzer.Adapter.GroupGridAdapter;
import com.empers.rssi_analyzer.database.GroupDB;
import com.empers.rssi_analyzer.objects.Group;

import java.util.ArrayList;
import java.util.List;

public class SelectionActivity extends AppCompatActivity {
    private final static String TAG = SelectionActivity.class.getSimpleName();
    private GridView mRecGrid;
    private GroupGridAdapter mAdapter;
    private GroupDB dataBase;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataBase.close();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        mRecGrid = (GridView)findViewById(R.id.node_grid);
        dataBase = new GroupDB(getApplicationContext());
        new AsyncTask<Void, Void, List<Group>>() {
            @Override
            protected List<Group> doInBackground(Void... params) {
                return dataBase.getAllGroups();
            }
            @Override
            protected void onPostExecute(final List<Group> groups) {
                super.onPostExecute(groups);
                mAdapter = new GroupGridAdapter(groups, SelectionActivity.this);
                mRecGrid.setAdapter(mAdapter);
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_edit).setVisible(false);
        menu.findItem(R.id.action_search_ble).setVisible(false);
        menu.findItem(R.id.action_pair).setVisible(false);
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_new) {
            View viewInflated = LayoutInflater.from(SelectionActivity.this)
                    .inflate(R.layout.dialog_name, (ViewGroup)findViewById(android.R.id.content), false);
            final EditText nameText = (EditText)viewInflated.findViewById(R.id.input_name);
            AlertDialog.Builder builder = new AlertDialog.Builder(SelectionActivity.this);
            builder.setView(viewInflated);
            builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(!nameText.getText().toString().isEmpty()) {
                        String inputName = nameText.getText().toString();
                        new AsyncTask<String, Void, List<Group>>() {
                            @Override
                            protected List<Group> doInBackground(String... params) {
                                Group group1 = new Group();
                                group1.setGroupName(params[0]);
                                group1.setGroupId((int)dataBase.addGroup(group1));
                                //mGroups.add(group1);
                                //db.close();
                                return dataBase.getAllGroups();
                            }
                            @Override
                            protected void onPostExecute(List<Group> groups) {
                                super.onPostExecute(groups);
                                mAdapter.updateData(groups);
                            }
                        }.execute(inputName);
                    }
                }
            });
            builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
        return super.onOptionsItemSelected(item);
    }
}
