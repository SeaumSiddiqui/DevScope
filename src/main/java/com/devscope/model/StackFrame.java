package com.devscope.model;

/**
 * One line in a stack trace.
 *
 * When an exception is thrown, Java records the full call chain — every method
 * that was executing at that moment. Each line in that chain is one StackFrame.
 *
 * Example: at com.example.OrderService.placeOrder(OrderService.java:81)
 *   className = "com.example.OrderService"
 *   methodName = "placeOrder"
 *   fileName = "OrderService.java"
 *   lineNumber = 81
 *   appFrame = true
 *
 * The appFrame flag is set by comparing className against devscope.base-package.
 * The UI uses it to highlight your code in amber and collapse framework frames.
 */
public class StackFrame {
    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;

    /**
     * True if this frame belongs to the developer's own code,
     * false for framework/JDK frames.
     * */
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
