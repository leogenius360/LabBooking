package com.testlab.labbooking.models;

import java.util.List;

public class Lab {
    private String id;
    private String name;
    private String description;
    private int capacity;
    private List<String> resources;
    private String location;
    private boolean isActive;

    public Lab() {
        // Empty constructor for Firestore
    }

    public Lab(String id, String name, String description, int capacity,
               List<String> resources, String location) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.resources = resources;
        this.location = location;
        this.isActive = true;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}