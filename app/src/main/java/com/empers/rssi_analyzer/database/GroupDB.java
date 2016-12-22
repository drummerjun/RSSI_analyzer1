package com.empers.rssi_analyzer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.empers.rssi_analyzer.objects.Group;
import com.empers.rssi_analyzer.objects.Node;

import java.util.ArrayList;
import java.util.List;

public class GroupDB extends SQLiteOpenHelper {
    private static final String TAG = GroupDB.class.getSimpleName();
    private static final int DB_VER = 1;
    private static final String TABLE_NAME = "groups";    //table name
    //column names
    private static final String KEY_ID = "_id";
    private static final String KEY_GROUP_NAME = "group_name";

    public GroupDB(Context context) {
        super(context, "groupDatabase", null, DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MP3_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_GROUP_NAME + " TEXT"
                + ")";
        db.execSQL(CREATE_MP3_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long addGroup(Group group) {
        long new_id;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_GROUP_NAME, group.getGroupName());
        new_id = db.insert(TABLE_NAME, null, values);
        db.close();
        return new_id;
    }

    public Group getGroup(int group_id) {
        SQLiteDatabase db = getReadableDatabase();
        Group group = new Group();
        Cursor c = db.query(TABLE_NAME, new String[] {
                        KEY_ID, // 0
                        KEY_GROUP_NAME, // 1
                }, KEY_ID + "=?",
                new String[] {String.valueOf(group_id)}, null, null, null, null);
        if(c != null) {
            c.moveToFirst();
            group.setGroupId(c.getInt(0));
            group.setGroupName(c.getString(1));
            c.close();
        }
        db.close();
        return group;
    }

    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        if(c.moveToFirst()) {
            do {
                Group group = new Group();
                group.setGroupId(c.getInt(0));
                group.setGroupName(c.getString(1));
                groups.add(group);
            } while(c.moveToNext());
        }
        db.close();
        c.close();
        return groups;
    }

    public void updateGroup(Group group) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_GROUP_NAME, group.getGroupName());
        db.update(TABLE_NAME, values, "_id=" + group.getGroupId(), null);
        db.close();
    }

    public void deleteGroup(int group_id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, KEY_ID + " = ?",
                new String[] { String.valueOf(group_id) });
        db.close();
    }
}
