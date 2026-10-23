package com.devscope.model;

import java.util.List;

/**
 * One captured exception, with its full cause chain and the log lines from
 * the same thread that ran right before it.
 */
public class ErrorEvent {
    private String id;
    private long timestamp;
    private String threadName;

    private String exceptionClass;   // fully qualified, e.g. "java.lang.NullPointerException"
    private String exceptionMessage;
    private List<StackFrame> frames;

    // "Caused by:" layers, index 0 is the direct cause, last one is the root cause
    private List<CausedBy> causeChain;

    // log lines from the same thread before the error, oldest to newest
    private List<LogEntry> correlatedLogs;

    // id of the log line that came with the exception, the UI highlights it. can be null
    private String triggerLogId;

    public ErrorEvent() {}

    public ErrorEvent(String id, long timestamp, String threadName, String exceptionClass, String exceptionMessage, List<StackFrame> frames, List<CausedBy> causeChain, List<LogEntry> correlatedLogs, String triggerLogId) {
        this.id = id;
        this.timestamp = timestamp;
        this.threadName = threadName;
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
        this.frames = frames;
        this.causeChain = causeChain;
        this.correlatedLogs = correlatedLogs;
        this.triggerLogId = triggerLogId;
    }

    public String getTriggerLogId() {
        return triggerLogId;
    }

    public void setTriggerLogId(String triggerLogId) {
        this.triggerLogId = triggerLogId;
    }

    public List<LogEntry> getCorrelatedLogs() {
        return correlatedLogs;
    }

    public void setCorrelatedLogs(List<LogEntry> correlatedLogs) {
        this.correlatedLogs = correlatedLogs;
    }

    public List<CausedBy> getCauseChain() {
        return causeChain;
    }

    public void setCauseChain(List<CausedBy> causeChain) {
        this.causeChain = causeChain;
    }

    public List<StackFrame> getFrames() {
        return frames;
    }

    public void setFrames(List<StackFrame> frames) {
        this.frames = frames;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    // one "Caused by:" layer, only makes sense inside an ErrorEvent
    public static class CausedBy {
        private String exceptionClass;
        private String exceptionMessage;
        private List<StackFrame> frames;

        public CausedBy() {}

        public CausedBy(String exceptionClass, String exceptionMessage, List<StackFrame> frames) {
            this.exceptionClass = exceptionClass;
            this.exceptionMessage = exceptionMessage;
            this.frames = frames;
        }

        public String getExceptionClass() {
            return exceptionClass;
        }

        public void setExceptionClass(String exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        public String getExceptionMessage() {
            return exceptionMessage;
        }

        public void setExceptionMessage(String exceptionMessage) {
            this.exceptionMessage = exceptionMessage;
        }

        public List<StackFrame> getFrames() {
            return frames;
        }

        public void setFrames(List<StackFrame> frames) {
            this.frames = frames;
        }
    }
}
