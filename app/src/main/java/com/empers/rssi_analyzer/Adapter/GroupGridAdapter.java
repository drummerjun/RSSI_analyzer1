package com.empers.rssi_analyzer.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.empers.rssi_analyzer.MainActivity;
import com.empers.rssi_analyzer.R;
import com.empers.rssi_analyzer.SelectionActivity;
import com.empers.rssi_analyzer.database.GroupDB;
import com.empers.rssi_analyzer.database.NodeDB;
import com.empers.rssi_analyzer.objects.Group;
import com.empers.rssi_analyzer.objects.Node;

import java.util.ArrayList;
import java.util.List;

public class GroupGridAdapter extends BaseAdapter {
    private Context context;
    private List<Group> mGroups;

    public GroupGridAdapter(List<Group> groups, Context context) {
        mGroups = groups;
        this.context = context;
    }

    public void updateData(List<Group> groups) {
        if(groups != null) {
            //List<Group> newGroups = new ArrayList<>();
            //newGroups.addAll(groups);
            mGroups.clear();
            mGroups.addAll(groups);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mGroups == null ? 0 : mGroups.size();
    }

    @Override
    public Group getItem(int position) {
        return mGroups.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mGroups.get(position).getGroupId();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.card_grid_group, null);
        Toolbar toolbar = (Toolbar)convertView.findViewById(R.id.card_toolbar);
        String title = context.getString(R.string.room) + " "
                + String.valueOf(mGroups.get(position).getGroupId());
        toolbar.setTitle(title);
        toolbar.inflateMenu(R.menu.card_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_edit) {
                    View viewInflated = LayoutInflater.from(context)
                            .inflate(R.layout.dialog_name,
                                    (ViewGroup)((Activity)context).findViewById(android.R.id.content),
                                    false
                            );
                    final EditText nameText = (EditText)viewInflated.findViewById(R.id.input_name);
                    String name = mGroups.get(position).getGroupName();
                    nameText.setText(name);

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setView(viewInflated);
                    builder.setPositiveButton(context.getString(android.R.string.ok),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!nameText.getText().toString().isEmpty()) {
                                String inputName = nameText.getText().toString();
                                mGroups.get(position).setGroupName(inputName);
                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        GroupDB db = new GroupDB(context.getApplicationContext());
                                        db.updateGroup(mGroups.get(position));
                                        db.close();
                                        return null;
                                    }
                                }.execute();
                                notifyDataSetChanged();
                            }
                        }
                    });
                    builder.setNegativeButton(context.getString(android.R.string.cancel),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                } else if(item.getItemId() == R.id.action_delete) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(context.getString(R.string.delete) + " "
                            + mGroups.get(position).getGroupName() + " ?");
                    builder.setPositiveButton(context.getString(android.R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... params) {
                                            int group_id = mGroups.get(position).getGroupId();
                                            GroupDB db = new GroupDB(context.getApplicationContext());
                                            db.deleteGroup(group_id);
                                            db.close();

                                            NodeDB nodeDB = new NodeDB(context.getApplicationContext());
                                            nodeDB.deleteNodes(group_id);
                                            nodeDB.close();
                                            return null;
                                        }

                                        @Override
                                        protected void onPostExecute(Void aVoid) {
                                            super.onPostExecute(aVoid);
                                            mGroups.remove(position);
                                            notifyDataSetChanged();
                                        }
                                    }.execute();
                                }
                            });
                    builder.setNegativeButton(context.getString(android.R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    builder.show();
                }
                return true;
            }
        });
        TextView tvGroupName = (TextView)convertView.findViewById(R.id.groupNameTextView);
        tvGroupName.setText(mGroups.get(position).getGroupName());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("GroupGridAdapter", "convertView onClick");
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("GROUP_ID", mGroups.get(position).getGroupId());
                intent.putExtra("GROUP_NAME", mGroups.get(position).getGroupName());
                context.startActivity(intent);
            }
        });
        return convertView;
    }
}
