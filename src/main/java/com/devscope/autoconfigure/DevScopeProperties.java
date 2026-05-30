package com.devscope.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Configuration properties for DevScope, bound from application.properties.
 *
 * All properties are optional except base-package, which should always be set
 * to the root package of your own code (e.g. com.example.myapp).
 *
 * Example configuration:
 *
 *   devscope.enabled=true
 *   devscope.base-package=com.example.myapp
 *   devscope.max-errors=200
 *   devscope.max-logs=1000
 */
@ConfigurationProperties(prefix = "devscope")
public class DevScopeProperties {
    /**
     * Master on/off switch. Defaults to true.
     * Set false to disable without removing the dependency.
     * */
    private boolean enabled = true;

    /**
     * Root package of the developer's own code.
     *
     * Any stack frame whose class name starts with this value is marked as an
     * app frame — highlighted in the UI and counted separately from framework
     * frames. Too broad a value (e.g. "com") flags framework classes as yours;
     * too narrow misses service and repository layers.
     */
    private String basePackage = "";

    /** Maximum ErrorEvents to hold in memory. Oldest is evicted when this limit is hit. */
    private int maxErrors = 200;

    /** Maximum LogEntry objects to hold in memory. Oldest is evicted when this limit is hit. */
    private int maxLogs = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public int getMaxErrors() {
        return maxErrors;
    }

    public void setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
    }

    public int getMaxLogs() {
        return maxLogs;
    }

    public void setMaxLogs(int maxLogs) {
        this.maxLogs = maxLogs;
    }
}