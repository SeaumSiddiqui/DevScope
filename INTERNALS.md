<div style="text-align: center;">
  <img src="assets/devscope_banner.svg" alt="DevScope UI" width="95%">
</div>


# DevScope Internals

This document is for contributors and anyone who wants to understand how DevScope works under the hood. If you just want to use it, see [README.md](README.md).

---

## The core problem this library solves (technically)

When a Spring Boot controller throws an exception, what actually happens is:

1. Your code throws `YourException`
2. Spring's `DispatcherServlet` catches it, wraps it in a `ServletException`, and logs it — but crucially, it logs the **root cause** as the attached throwable, not the outermost exception
3. Logback fires a log event with the root cause attached
4. Your terminal shows `IOException: Socket timed out` when what you actually threw was `OrderServiceException: Could not process order`

This is the core UX problem. The terminal always shows the wrong exception as the headline. DevScope intercepts the exception **before** Spring's error handling strips the wrapper chain, preserving the full picture.

---

## Package structure

```
com.devscope
├── model/
│   ├── StackFrame.java         one line in a stack trace
│   ├── LogEntry.java           one captured log line
│   └── ErrorEvent.java         one captured exception with full context
├── store/
│   └── ErrorEventStore.java    in-memory ring buffer for events
├── appender/
│   ├── DevScopeAppender.java         Logback hook — captures log lines
│   └── DevScopeAppenderLifecycle.java  wires the appender to Logback at startup
├── web/
│   ├── DevScopeRequestFilter.java    servlet filter — captures exceptions
│   ├── DevScopeController.java       REST API endpoints
│   └── DevScopeUiController.java     serves the HTML UI
└── autoconfigure/
    ├── DevScopeAutoConfiguration.java  Spring Boot auto-config entry point
    └── DevScopeProperties.java         config property bindings
```

---

## Data flow

Every request that throws an exception goes through this path:

```
HTTP request
  → DevScopeRequestFilter (catches raw exception, builds ErrorEvent)
      → stores ErrorEvent in ErrorEventStore
  → Spring error handling continues normally (returns error response to client)

Every log statement (including the above exception's log event) goes through:
  → DevScopeAppender.append()
      → builds LogEntry, stores in ErrorEventStore
```

The UI polls `/dev-scope/api/errors` and `/dev-scope/api/logs` every 3 seconds and renders whatever is in the store.

---

## Why two separate capture mechanisms

A natural question: why have both a servlet filter AND a Logback appender? Why not just the appender?

The appender was the original design. It worked until we hit a fundamental Logback behavior: when Spring logs a wrapped exception, it attaches the **innermost** cause as the throwable on the log event — not the exception your code threw. So the appender always captured the wrong exception as the primary entry.

The servlet filter sits above Spring's entire error handling chain. It catches the exception in its raw original form, with the full wrapper chain intact, before Spring touches it. This is the only reliable way to get the correct outermost exception.

The appender stayed because it's still the right tool for capturing log lines. Log lines don't have this problem — a log line is just a string. The appender captures all of them cleanly and efficiently.

So the split is intentional: filter captures exceptions, appender captures logs.

---

## ErrorEventStore — the ring buffer

`ErrorEventStore` is the single shared data structure. It holds two `ArrayDeque` collections — one for `ErrorEvent`, one for `LogEntry` — each with a configurable maximum size.

When either deque reaches capacity, the oldest entry is evicted from the front before the newest is added to the back. This is the ring buffer pattern — fixed memory footprint regardless of how long the app runs or how many exceptions it throws.

Thread safety uses `ReentrantReadWriteLock`. Multiple threads can read simultaneously (the poll cycle, the REST controller) but writes are exclusive. The lock is always released in a `finally` block so a write exception never deadlocks future operations.

Duplicate detection lives in `addError`. The same exception arriving within 500ms on the same thread with the same class and message is rejected. This guards against the servlet container re-dispatching error requests through the filter chain a second time.

---

## DevScopeRequestFilter — exception capture

Extends Spring's `OncePerRequestFilter`. The filter wraps the entire request in a try-catch. When an exception escapes, it:

1. Calls `unwrap()` to strip `ServletException` and `NestedServletException` wrappers, getting to the real exception your code threw
2. Parses the stack trace into `StackFrame` objects, flagging frames whose class name starts with `devscope.base-package` as app frames
3. Walks the `getCause()` chain recursively to build the `CausedBy` list
4. Calls `store.getRecentLogsByThread()` to attach the log lines that ran on this thread before the exception
5. Stores the `ErrorEvent` and rethrows the original exception so Spring can still handle the HTTP error response normally

The rethrow is important. DevScope never swallows exceptions or changes the response your app sends.

---

## DevScopeAppender — log capture

Extends Logback's `AppenderBase<ILoggingEvent>`. The `append()` method is called synchronously on the application thread for every log event. It builds a `LogEntry` and writes it to the store.

This method must be fast — it runs on every log call in the entire application. It does nothing but build a POJO and call a synchronized store method. No I/O, no parsing, no blocking.

The appender is attached to Logback's root logger in `DevScopeAppenderLifecycle.attach()`, which is called via `@PostConstruct` after Spring constructs the bean. Attaching to root means all child loggers — every class in your app — automatically route through this appender without any per-logger configuration.

---

## Auto-configuration — how Spring finds DevScope

Spring Boot reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` from every jar on the classpath at startup. Our file contains one line pointing to `DevScopeAutoConfiguration`.

Spring then evaluates the conditions on that class:
- `@ConditionalOnWebApplication` — skip entirely if there's no embedded server
- `@ConditionalOnProperty(name = "devscope.enabled", matchIfMissing = true)` — active by default, opt-out via property

If both pass, Spring processes the `@Bean` methods and registers `ErrorEventStore`, `DevScopeController`, `DevScopeUiController`, `DevScopeAppenderLifecycle`, and the `FilterRegistrationBean` for `DevScopeRequestFilter`.

The `@PostConstruct` that attaches the Logback appender lives on `DevScopeAppenderLifecycle` rather than `DevScopeAutoConfiguration` itself. This is deliberate — Spring Boot 3 prohibits circular bean dependencies, and a `@PostConstruct` on a `@Configuration` class that references its own `@Bean` methods creates a self-referential cycle Spring refuses to resolve.

---

## The UI

A single vanilla HTML/CSS/JS file served from `META-INF/resources/dev-scope/index.html`. Spring Boot automatically serves static resources from this path in library jars — no controller needed for the file itself, though we have `DevScopeUiController` to handle the `/dev-scope` and `/dev-scope/` routes explicitly.

The UI polls three endpoints every 3 seconds: `/stats`, `/errors`, and `/logs`. It uses `Promise.all` so all three fetches happen in parallel.

The detail drawer is only built once when you click an error. During subsequent polls, only the timestamp span is updated — the drawer body is never rebuilt. This preserves all expanded/collapsed state across refresh cycles. The scroll position of the drawer body is saved before DOM updates and restored after, preventing the browser from jumping back to the top on each poll.

Framework frames in the stack trace viewer are toggled by replacing only the frames section's `innerHTML`, not by rebuilding the entire drawer.

---

## appFrame detection

Every `StackFrame` has a boolean `appFrame` field. It is `true` if the frame's class name starts with the configured `devscope.base-package`.

This single boolean drives most of the UI's intelligence — the amber highlighting, the default collapse of framework frames, the "app frame hint" on the error list entry, and the per-layer app frame count badges on the cause chain.

Setting `base-package` correctly is the most important configuration step. Too broad (e.g. `com`) and framework classes get highlighted. Too narrow (e.g. `com.example.myapp.controllers`) and frames from your service and repository layers are treated as framework noise.

---

## Known limitations

**Correlated logs are best-effort.** The filter captures the exception, then immediately calls `getRecentLogsByThread` to fetch log lines from the same thread. This works well in practice but there is a small window between when the log line is written and when the filter runs — in theory a very fast exception could arrive before its own log event is stored. In practice this has not been observed.

**In-memory only.** All data is lost on restart. This is intentional for v1 — the library targets local and staging development, not long-running production monitoring.

**One base-package.** If your codebase spans multiple root packages, only one can be configured. Multi-package support is a planned improvement.

---

## Adding a feature — the right path

Any new data that should appear in the UI needs to follow this path:

1. Add a field to the relevant model class (`ErrorEvent`, `LogEntry`, or `StackFrame`)
2. Populate it in `DevScopeRequestFilter` (for error data) or `DevScopeAppender` (for log data)
3. The REST controller serializes model objects directly — no changes needed there unless you're adding a new endpoint
4. Update the UI in `index.html` to render the new field

The store, auto-configuration, and controller are intentionally thin. Business logic belongs in the filter and appender, not scattered across the stack.
