package com.devscope.model;

/**
 * One line of a stack trace, e.g.
 *   at com.example.OrderService.placeOrder(OrderService.java:81)
 */
public class StackFrame {
    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;

    // true when className starts with devscope.base-package (your code, not the framework)
    private boolean appFrame;

    public StackFrame() {}

    public StackFrame(String className, String methodName, String fileName, int lineNumber, boolean appFrame) {
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.appFrame = appFrame;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isAppFrame() {
        return appFrame;
    }

    public void setAppFrame(boolean appFrame) {
        this.appFrame = appFrame;
    }
}
