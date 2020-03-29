/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.softwareco.intellij.plugin;

import com.google.gson.JsonObject;
import com.softwareco.intellij.plugin.models.ResourceInfo;

public class KeystrokeProject {

    private String name;
    private String directory;
    private String identifier;
    private ResourceInfo resource = new ResourceInfo();

    public KeystrokeProject(String name, String directory) {
        this.name = name;
        this.directory = directory;
        ResourceInfo resourceInfo = SoftwareCoUtils.getResourceInfo(directory);
        if (resourceInfo != null) {
            this.resource.setIdentifier(resourceInfo.getIdentifier());
            this.resource.setTag(resourceInfo.getTag());
            this.resource.setBranch(resourceInfo.getBranch());
            this.resource.setEmail(resourceInfo.getEmail());
            this.identifier = resourceInfo.getIdentifier();
        }
    }

    public void resetData() {
        // intentional for now
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() { return identifier; }

    public void updateResource(ResourceInfo resource) {
        this.resource = resource;
    }

    public boolean hasResource() {
        return this.resource != null && !this.resource.getIdentifier().isEmpty();
    }

    public String getResource() {
        return SoftwareCo.gson.toJson(resource);
    }

    @Override
    public String toString() {
        return "KeystrokeProject{" +
                "name='" + name + '\'' +
                ", directory='" + directory + '\'' +
                '}';
    }
}
