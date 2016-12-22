package com.empers.rssi_analyzer.objects;

import java.util.List;

/**
 * Created by JunyenHuang on 12/15/2016.
 */

public class Group {
    private int id;
    private String name;
    private List<Node> nodes;

    public void setGroupId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return id;
    }

    public void setGroupName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return name;
    }

    public void setNodes(List<Node> list) {
        nodes = list;
    }

    public List<Node> getNodes() {
        return nodes;
    }
}
