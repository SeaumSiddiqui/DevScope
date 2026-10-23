package com.devscope.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from application.properties, e.g.
 *
 *   devscope.enabled=true
 *   devscope.base-package=com.example.myapp
 *   devscope.max-errors=200
 *   devscope.max-logs=1000
 */
@ConfigurationProperties(prefix = "devscope")
public class DevScopeProperties {
    /** Master switch, set false to disable without removing the dependency. */
    private boolean enabled = true;

    /**
     * Root package of your own code. Stack frames starting with this are
     * marked as app frames and highlighted in the UI, everything else is
     * treated as framework noise.
     */
    private String basePackage = "";

    /** Max errors kept in memory, oldest is dropped when full. */
    private int maxErrors = 200;

    /** Max log lines kept in memory, oldest is dropped when full. */
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
