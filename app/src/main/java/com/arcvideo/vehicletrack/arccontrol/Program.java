package com.arcvideo.vehicletrack.arccontrol;

public class Program {
    private String id;
    private String currentPlayType;
    private String mediaType;
    private String type;
    private String name;

    public Program(String id, String currentPlayType, String mediaType, String type, String name) {
        this.id = id;
        this.currentPlayType = currentPlayType;
        this.mediaType = mediaType;
        this.type = type;
        this.name = name;
    }
    public Program(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Program{" +
                "id=" + id +
                ", currentPlayType='" + currentPlayType + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
