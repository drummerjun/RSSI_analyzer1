package com.empers.rssi_analyzer.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.empers.rssi_analyzer.Constants;
import com.empers.rssi_analyzer.R;
import com.empers.rssi_analyzer.database.NodeDB;
import com.empers.rssi_analyzer.objects.Node;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NodeAdapter extends RecyclerView.Adapter<NodeAdapter.NodeViewHolder> {
	private static final String TAG = NodeAdapter.class.getSimpleName();
	private List<Node> mNodes;
    private Context context;

	public NodeAdapter(Context context, List<Node> array){
        this.context = context;
        mNodes = array;
        Collections.sort(mNodes, new NodeComparator());
	}
	
	@Override
	public NodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.card_scan, parent, false);
        return new NodeViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(final NodeViewHolder holder, int position) {
        if(mNodes == null) {
            return;
        }
        Node node = mNodes.get(position);
        holder.cardToolbar.setTitle(node.getNodeId() + " " + node.getDisplayName());
        holder.cardToolbar.getMenu().clear();
        holder.cardToolbar.inflateMenu(R.menu.card_menu);
        holder.cardToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_edit) {
                    View viewInflated = LayoutInflater.from(context)
                            .inflate(R.layout.dialog_new_node,
                                    (ViewGroup)((Activity)context).findViewById(android.R.id.content),
                                    false
                            );
                    final NumberPicker nodePicker = (NumberPicker)viewInflated.findViewById(R.id.numberPicker);
                    nodePicker.setMinValue(1);
                    nodePicker.setMaxValue(255);
                    nodePicker.setValue(mNodes.get(holder.getAdapterPosition()).getNodeId());

                    final EditText nameText = (EditText)viewInflated.findViewById(R.id.input_name);
                    String name = mNodes.get(holder.getAdapterPosition()).getDisplayName();
                    nameText.setText(name);

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setView(viewInflated);
                    builder.setPositiveButton(context.getString(android.R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(!nameText.getText().toString().isEmpty()) {
                                        String inputName = nameText.getText().toString();
                                        mNodes.get(holder.getAdapterPosition()).setDisplayName(inputName);
                                        mNodes.get(holder.getAdapterPosition()).setNodeId(nodePicker.getValue());
                                        Collections.sort(mNodes, new NodeComparator());
                                        new AsyncTask<Integer, Void, Void>() {
                                            NodeDB db = new NodeDB(context.getApplicationContext());
                                            @Override
                                            protected Void doInBackground(Integer... params) {
                                                db.updateNode(mNodes.get(params[0]));
                                                return null;
                                            }

                                            @Override
                                            protected void onPostExecute(Void aVoid) {
                                                super.onPostExecute(aVoid);
                                                db.close();
                                            }
                                        }.execute(holder.getAdapterPosition());
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
                            + mNodes.get(holder.getAdapterPosition()).getDisplayName() + " ?");
                    builder.setPositiveButton(context.getString(android.R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new AsyncTask<Integer, Void, Void>() {
                                        NodeDB db = new NodeDB(context.getApplicationContext());
                                        @Override
                                        protected Void doInBackground(Integer... params) {
                                            db.deleteNode(mNodes.get(params[0]).getID());
                                            return null;
                                        }
                                        @Override
                                        protected void onPostExecute(Void aVoid) {
                                            super.onPostExecute(aVoid);
                                            mNodes.remove(holder.getAdapterPosition());
                                            notifyDataSetChanged();
                                            db.close();
                                        }
                                    }.execute(holder.getAdapterPosition());
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

        int value = node.getSignalStrength();
        holder.tvResultCircle.setText(String.valueOf(value));
        if(value >= 66) {
            holder.tvResultCircle.setBackgroundResource(R.drawable.selector_circular_green);
        } else if (value >= 33 && value < 66) {
            holder.tvResultCircle.setBackgroundResource(R.drawable.selector_circular_orange);
        } else if (value > 0 && value < 33) {
            holder.tvResultCircle.setBackgroundResource(R.drawable.selector_circular_red);
        } else {
            holder.tvResultCircle.setBackgroundResource(R.drawable.selector_circular_background);
        }

//		String data=String.format("%d%%/%d dBm",gZNodes.get(position).value,gZNodes.get(position).rssi);
        //	holder.signal.setText(gZNodes.get(position).status);
//		text=gZNodes.get(position).status;

        if(node.isScanning()){
            holder.tvResultCircle.setVisibility(View.INVISIBLE);
            holder.aviView.setVisibility(View.VISIBLE);
            holder.tvHint.setText(R.string.scanning);
            Animation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(500); //blinking time
            anim.setStartOffset(20);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            holder.tvHint.startAnimation(anim);
        }else{
            holder.tvResultCircle.setVisibility(View.VISIBLE);
            holder.aviView.setVisibility(View.GONE);
            holder.tvHint.setText(R.string.hint_scan);
            holder.tvHint.clearAnimation();
        }
	}

    @Override
	public int getItemCount() {
        return mNodes == null ? 0 : mNodes.size();
	}

    public void updateData(List<Node> nodes) {
        if(nodes != null) {
            mNodes.clear();
            mNodes.addAll(nodes);
            Collections.sort(mNodes, new NodeComparator());
            notifyDataSetChanged();
        }
    }

    private class NodeComparator implements Comparator<Node> {
        @Override
        public int compare(Node left, Node right) {
            return left.getNodeId() - right.getNodeId();
        }
    }

	// return the list of dat
	public List<Node> getNodes(){
		return mNodes;
	}

	public class NodeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private Toolbar cardToolbar;
		private TextView tvHint, tvResultCircle;
        private AVLoadingIndicatorView aviView;
		public NodeViewHolder(View v) {
			super(v);
            cardToolbar = (Toolbar)v.findViewById(R.id.card_toolbar);
            tvHint = (TextView)v.findViewById(R.id.hintTextView);
            tvResultCircle = (TextView)v.findViewById(R.id.result);
            aviView = (AVLoadingIndicatorView)v.findViewById(R.id.avi);
			v.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
            Node node = mNodes.get(getAdapterPosition());
            mNodes.get(getAdapterPosition()).setScanning(true);
            notifyDataSetChanged();
            testNodeSignal(node.getNodeId());
		}
	}

    private void testNodeSignal(int nodeID){
        String msg = String.format(Locale.getDefault(), "{\"CMD\":\"scan\",\"ID\":%d}", nodeID);
        Intent intent = new Intent(Constants.ACTION_SCAN);
        intent.putExtra("NODE_ID", nodeID);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        //mBluetooth.send(msg,true);
        //if(app.setSCAN(nodeID, true)) {
        //    notifyDataSetChanged();
        //}
    }

    public void setNodeScan(int node_id, boolean isScanning, int rssi) {
        for (int i = 0; i < mNodes.size(); i++) {
            Node node = mNodes.get(i);
            if(node.getNodeId() == node_id) {
                mNodes.get(i).setScanning(isScanning);
                mNodes.get(i).setSignalStrength(rssiToStrength(rssi));
                notifyDataSetChanged();
                return;
            }
        }
    }

    private int rssiToStrength(int rssi){
        int val=0;
        if(rssi > 0){
            val=0;
        }else if(rssi >= -50){
            val=100;
        }else if(rssi<=-100){
            val=0;
        }else if( rssi > -100 && rssi < -50){
            val = 2*(rssi+100);
        }
        Log.i(TAG,String.format("val=%d",val));
        return val;
    }
}
