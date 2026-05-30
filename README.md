# DevScope

A zero-config Spring Boot library that gives you a live error inspection UI at `/dev-scope` — no infrastructure, no ELK stack, no setup beyond one dependency.

When something breaks, you open the browser instead of squinting at a terminal wall.

![DevScope UI](assets/devscope_ui.png)

---

## The problem it solves

Spring Boot's terminal output is honest but brutal. A single exception produces 50+ lines of stack trace, most of it framework internals that have nothing to do with your bug. Finding the one line in your own code, correlating it with the log lines that ran before it, and understanding the full cause chain — that's all manual work today.

DevScope captures every exception your app throws, parses it, and presents it in a structured UI with your code highlighted, framework noise collapsed, and the log context attached.

---

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.devscope</groupId>
    <artifactId>spring-devscope</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Add two lines to your `application.properties` or `application.yml`:

```properties
devscope.enabled=true
devscope.base-package=com.example.yourapp
```

Start your application and open `http://localhost:8080/dev-scope`.

That's it. No `@EnableDevScope`, no XML, no beans to declare.

---

## Configuration

All properties are optional except `base-package`, which you should always set.

| Property | Default | Description |
|---|---|---|
| `devscope.enabled` | `true` | Master switch. Set `false` to disable without removing the dependency. |
| `devscope.base-package` | `""` | Root package of your own code — e.g. `com.example.myapp`. Used to distinguish your frames from framework frames in stack traces. |
| `devscope.max-errors` | `200` | Maximum errors to keep in memory. Oldest is evicted when full. |
| `devscope.max-logs` | `1000` | Maximum log lines to keep in memory. |

For profile-specific activation (recommended), put the config in `application-dev.properties`:

```properties
# application-dev.properties
devscope.enabled=true
devscope.base-package=com.example.myapp
```

This way DevScope never accidentally runs in production.

---

## What you get

**Error feed** — every exception your app throws appears in the left panel, newest first. Each entry shows the exception class, message, timestamp, thread, and the exact line in your code where it originated.

**Stack trace viewer** — clicking an error opens a detail drawer. Your code frames are highlighted in amber. The 50+ Spring and JDK frames are collapsed by default with a toggle to expand them when you need them.

**Cause chain** — wrapped exceptions ("Caused by:") are rendered as nested collapsible layers so you can follow the chain from the outermost wrapper to the root cause without scrolling through a flat wall of text.

**Correlated logs** — the log lines from the same thread in the seconds before the error are attached to each error entry. This is the context that explains what the app was doing when it broke.

**Live log stream** — the right panel shows all captured log lines, newest first, with level filters (ERROR, WARN, INFO, DEBUG).

**Active profile display** — the current Spring profile is shown in the header. Useful for diagnosing "works on my machine" issues where the culprit is usually a profile difference.

---

## API endpoints

The UI consumes these endpoints internally. They're also available directly if you want to integrate with other tools.

```
GET       /dev-scope/api/errors            >> all captured errors, newest first
GET       /dev-scope/api/errors/{id}       >> single error by ID
GET       /dev-scope/api/logs              >> all captured logs, newest first
GET       /dev-scope/api/logs?level=ERROR  >> filtered by level
GET       /dev-scope/api/stats             >> error count, log count, active profiles
DELETE    /dev-scope/api/clear             >> wipe all stored data
```

---

## Safety

DevScope is in-memory only. Nothing is written to disk, no external connections are made, no data leaves your process. Captured events are evicted automatically when the store reaches capacity.

It activates only in web applications (`@ConditionalOnWebApplication`). Adding it to a batch job or CLI app has no effect.

For production safety, always scope it to a dev profile as shown above, or set `devscope.enabled=false` in your production properties.

---

## Requirements

- Java 17 or higher
- Spring Boot 3.x
- An embedded web server (Tomcat, Jetty, or Undertow)

---

## Contributing

See [INTERNALS.md](INTERNALS.md) for architecture decisions, data flow, and a guide to the codebase.
