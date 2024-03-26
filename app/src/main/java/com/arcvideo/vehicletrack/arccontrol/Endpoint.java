package com.arcvideo.vehicletrack.arccontrol;

public class Endpoint {
    private String name;
    private String id;
    private String state;
    private String ip;


    public Endpoint(String name, String id, String state, String ip) {
        this.name = name;
        this.id = id;
        this.state = state;
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", state='" + state + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
