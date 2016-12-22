package com.empers.rssi_analyzer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.empers.rssi_analyzer.objects.Group;
import com.empers.rssi_analyzer.objects.Node;

import java.util.ArrayList;
import java.util.List;

public class NodeDB extends SQLiteOpenHelper {
    private static final String TAG = NodeDB.class.getSimpleName();
    private static final int DB_VER = 1;
    private static final String TABLE_NAME = "nodes_table";    //table name
    //column names
    private static final String KEY_ID = "_id";
    private static final String KEY_NAME = "node_name";
    private static final String KEY_NODE_ID = "node_id";
    private static final String KEY_GROUP_ID = "group_id";

    public NodeDB(Context context) {
        super(context, "nodesDatabase", null, DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MP3_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_NODE_ID + " INTEGER,"
                + KEY_GROUP_ID + " INTEGER"
                + ")";
        db.execSQL(CREATE_MP3_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long addNode(Node node) {
        long new_id;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, node.getDisplayName());
        values.put(KEY_NODE_ID, node.getNodeId());
        values.put(KEY_GROUP_ID, node.getGroupId());
        new_id = db.insert(TABLE_NAME, null, values);
        db.close();
        return new_id;
    }

    public Node getNode(int node_id) {
        SQLiteDatabase db = getReadableDatabase();
        Node node = new Node();
        Cursor c = db.query(TABLE_NAME, new String[] {
                        KEY_ID, // 0
                        KEY_NAME, // 1
                        KEY_NODE_ID, // 2
                        KEY_GROUP_ID // 3
                }, KEY_NODE_ID + "=?",
                new String[] {String.valueOf(node_id)}, null, null, null, null);
        if(c != null) {
            c.moveToFirst();
            node.setID(c.getInt(0));
            node.setDisplayName(c.getString(1));
            node.setNodeId(c.getInt(2));
            node.setGroupId(c.getInt(3));
            c.close();
        }
        db.close();
        return node;
    }

    public void updateNode(Node node) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, node.getDisplayName());
        values.put(KEY_NODE_ID, node.getNodeId());
        values.put(KEY_GROUP_ID, node.getGroupId());
        db.update(TABLE_NAME, values, "_id=" + node.getID(), null);
        db.close();
    }

    public ArrayList<Node> getNodes(int group_id) {
        ArrayList<Node> nodeList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_NAME
                + " WHERE " + KEY_GROUP_ID + "=" + String.valueOf(group_id);

        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if(c.moveToFirst()) {
            do {
                Node node1 = new Node();
                node1.setID(c.getInt(0));
                node1.setDisplayName(c.getString(1));
                node1.setNodeId(c.getInt(2));
                node1.setGroupId(c.getInt(3));
                nodeList.add(node1);
            } while(c.moveToNext());
        }
        db.close();
        c.close();
        return nodeList;
    }

    public void deleteNodes(int group_id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, KEY_GROUP_ID + " = ?",
                new String[] { String.valueOf(group_id) });
        db.close();
    }

    public void deleteNode(int _id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, KEY_ID + " = ?",
                new String[] { String.valueOf(_id) });
        db.close();
    }
}
