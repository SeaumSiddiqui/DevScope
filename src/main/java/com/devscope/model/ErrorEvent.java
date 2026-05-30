package com.devscope.model;

import java.util.List;

/**
 * One captured exception — the central object of this library.
 *
 * Created by {@link com.devscope.web.DevScopeRequestFilter} when it intercepts
 * a raw exception thrown by the application, before Spring's error handling
 * strips the wrapper chain.
 *
 * Contains the exception itself, its full cause chain, and the log lines from
 * the same thread that ran in the seconds before it fired.
 *
 * WHY CAUSE CHAIN:
 * Java exceptions are often wrapped — a ServiceException wrapping a
 * RepositoryException wrapping a SQLException. We store the whole chain so the
 * UI can render it as nested collapsible layers rather than a flat wall of text.
 *
 * WHY CORRELATED LOGS:
 * The log lines from the same thread before the error are the story of what the
 * app was doing. A developer reading "NullPointerException in placeOrder" needs
 * to know which order request triggered it, which user, which product. That
 * context is in the preceding log lines, and we attach it automatically.
 */
public class ErrorEvent {
    private String id;
    private long timestamp;
    private String threadName;

    private String exceptionClass;   // fully qualified, e.g. "java.lang.NullPointerException"
    private String exceptionMessage;
    private List<StackFrame> frames;

    /**
     * The "Caused by:" chain. Index 0 is the direct cause of the top-level
     * exception; the last index is the root cause.
     */
    private List<CausedBy> causeChain;

    /**
     * Log lines from the same thread captured before this exception fired,
     * ordered oldest to newest.
     */
    private List<LogEntry> correlatedLogs;

    /**
     * ID of the LogEntry that accompanied this exception in the log output.
     * The UI uses this to highlight that specific line in the correlated logs list.
     * Null if the error was captured by the filter without a matching log event.
     */
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


    /**
     * One "Caused by:" layer in the exception chain.
     * Nested here because it has no meaning outside an ErrorEvent.
     */
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
