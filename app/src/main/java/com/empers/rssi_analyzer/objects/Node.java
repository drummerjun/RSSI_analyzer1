package com.empers.rssi_analyzer.objects;

public class Node {
    private int _id;
    private boolean selected;
    private int node_id;
    private String name;
    private int group_id;
    private int signal_strength;
    private boolean scanning;

    public Node() {
        _id = 1;
        selected = false;
        node_id = 0;
        name = "";
        group_id = 0;
        signal_strength = 0;
        scanning = false;
    }

    public void setID(int id) {
        _id = id;
    }

    public int getID() {
        return _id;
    }

    public void setSelected(boolean value) {
        selected = value;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setNodeId(int i) {
        node_id = i;
    }

    public int getNodeId() {
        return node_id;
    }

    public void setDisplayName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return name;
    }

    public void setGroupId(int i) {
        group_id = i;
    }

    public int getGroupId() {
        return group_id;
    }

    public void setSignalStrength(int val) {
        signal_strength = val;
    }

    public int getSignalStrength() {
        return signal_strength;
    }

    public void setScanning(boolean value) {
        scanning = value;
    }

    public boolean isScanning() {
        return scanning;
    }
}
