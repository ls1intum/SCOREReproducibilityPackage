# Reproducibility Package

This repository contains a small but comprehensive toolkit for reproducing and monitoring access to protected resources on a typical JVM.  The code focuses on four areas that are commonly restricted in production systems:

| Area | Entry Points |
| ---- | ------------ |
| Command execution | `CommandSystemExecutionAccess` |
| File system operations | `FileSystemCreateAccess`, `FileSystemDeleteAccess`, `FileSystemExecuteAccess`, `FileSystemReadAccess`, `FileSystemWriteAccess` |
| Network sockets | `NetworkSystemConnectAccess`, `NetworkSystemReceiveAccess`, `NetworkSystemSendAccess` |
| Thread creation | `ThreadSystemCreateAccess`, `ThreadToCreate`, `RunnableToCreate` |

Each *Access* class inherits from `ProtectedRessourceAccess`, ensuring a shared messaging contract and a consistent set of helper utilities for threads, sockets, and process orchestration.  The goal is to make it easy to replay and observe the exact API calls your environment must audit or block.

> ⚠️ **Safety note:** A few demos execute system commands or open local network sockets. Run the package only on machines where you have the right to perform these actions.

---

## Prerequisites

- JDK 21 (earlier LTS releases may work, but the wrapper is configured for Gradle 9.2 which assumes a recent JDK).
- bash/zsh or PowerShell for running the Gradle wrapper.

Everything else (Gradle itself and dependencies such as JUnit 5) is resolved through the wrapper.

---

## Building and Testing

```bash
./gradlew clean build   # compiles main sources and runs the JUnit test suite
./gradlew test          # run tests only
./gradlew jar           # package compiled classes as a jar in build/libs
```

Gradle stores its caches in `.gradle/` and outputs (class files, reports, jars) under `build/`.  The default tasks currently exercise the Javadoc and style conventions but do not ship a runnable CLI entry point; see below for ways to invoke the demos manually.

---

## Running Specific Demos

Each `accessProtectedRessourceById(int)` method maps an ID to a specific API call.  You can trigger demos from JShell, your IDE, or a tiny driver class.  Example:

```bash
./gradlew classes
jshell <<'EOF'
import de.tum.cit.aet.FileSystemReadAccess;
var reader = new FileSystemReadAccess();
System.out.println(reader.accessProtectedRessourceById(1));
EOF
```

Similarly, you can instantiate `NetworkSystemReceiveAccess` to simulate TCP/UDP reads, or `CommandSystemExecutionAccess` to validate process-spawning controls.  Refer to each class’ switch statement for the available IDs and the corresponding API.

If you prefer a custom driver, edit `src/main/java/de/tum/cit/aet/Main.java`, wire the desired access class, and run it with:

```bash
./gradlew -q run --args='…'   # if you add the application plugin
# or
java -cp build/classes/java/main de.tum.cit.aet.Main
```

---

## Repository Layout & Resources

```
src/
 ├─ main/java/de/tum/cit/aet/
 │   ├─ CommandSystemExecutionAccess.java     # Runtime/ProcessBuilder demos
 │   ├─ FileSystem*Access.java                # File create/read/write/delete/execute flows
 │   ├─ NetworkSystem*Access.java             # Connect/send/receive flows
 │   ├─ ThreadSystemCreateAccess.java         # Executors, streams, parallel helpers
 │   └─ ProtectedRessourceAccess.java         # Shared helpers, socket servers, etc.
 ├─ main/resources/                          # Sample files used by the demos
 └─ test/                                    # (Add tests here)
```

The `resources/` directory at the project root contains example payloads that the access classes expect (e.g., `resources/FileToRead.txt`).  Update those files or add new resource definitions before triggering the corresponding demos.
