package com.devscope.model;

/**
 * One captured log line.
 *
 * Created by {@link com.devscope.appender.DevScopeAppender} for every log event
 * the application produces — regardless of level.
 *
 * threadName is the key field for correlation. In a web app each HTTP request
 * runs on its own thread (e.g. "http-nio-8080-exec-3"), so all log lines sharing
 * a thread name with a given error are the story of what the app was doing before
 * that error fired. {@link com.devscope.store.ErrorEventStore#getRecentLogsByThread}
 * uses this to attach context logs to each ErrorEvent.
 */
public class LogEntry {
    private String id;
    private long timeStamp;
    private String level; // ERROR, WARN, INFO, DEBUG
    private String loggerName; // fully qualified, e.g. "com.example.OrderService"
    private String message;

    /**
     * The threadName field is critical, it's how we correlate log lines
     * to a specific error. In a web app, each HTTP request runs on its own
     * thread (e.g. "http-nio-8080-exec-3"). So all log lines from the same
     * thread as an exception are the "story" of what led to that error.
     */
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
