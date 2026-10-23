package com.devscope.model;

/**
 * One captured log line, any level.
 */
public class LogEntry {
    private String id;
    private long timeStamp;
    private String level; // ERROR, WARN, INFO, DEBUG
    private String loggerName; // fully qualified, e.g. "com.example.OrderService"
    private String message;

    // how we match log lines to an error: in a web app each request runs on its
    // own thread (e.g. "http-nio-8080-exec-3"), so same thread = same request
    private String threadName;

    public LogEntry() {}

    public LogEntry(String id, long timeStamp, String level, String loggerName, String message, String threadName) {
        this.id = id;
        this.timeStamp = timeStamp;
        this.level = level;
        this.loggerName = loggerName;
        this.message = message;
        this.threadName = threadName;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
