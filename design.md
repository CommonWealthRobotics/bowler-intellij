# Plugin Design

## Functional Requirements

### Create a Project

- Users can create a new project using IntelliJ's project creation dialog
  - Projects can be libraries (no robot config file) or robots (one robot config file)
  - A new Git repo will be initialized in the project directory
    - A gitignore file will be added that has the relevant entries for our software, IDEs (e.g., Intellij, Eclipse, vscode), and common technologes (e.g., Java, Gradle)

### Launch Robots

- Users can create a run configuration that loads a robot via its config file.
  - Users can add dev'd dependencies by repo or local file path (only supported for dependencies on other library projects).
    - Adding via a repo should clone the repo into a local cache and then always resolve to that cache. Users can edit the repo inside the cache.
    - Adding via a local file path simply resolves to that path. Users can edit the files under that path.
  - Dev'ing a Maven dependency is not officially supported.
    - ! RFC: Maven local should work with Grapes if the kernel runs on the same machine as the Maven local repository. We should investigate this. If it works, then we should officially support this case (and still not support this if the kernel is running on a different machine than the Maven local repository).
  - Users can choose whether the kernel should connect to real hardware or to a simulator.
    - If hardware is selected, the user must supply a connection method (e.g., USB HID, UDP).
    - If a simulator is selected, the user must select which simulator implementation (e.g., Bullet, PhysX). The IDE must ask the kernel what simulator plugins are available.
- A connection method must also be specified.
  - The user can construct a list of connection methods that are ordered by preference.

### Run Scripts

- Users can create a run configuration that runs a script (this is just the default Groovy run configuration).
  - If the user runs in the debug configuration, the IDE should automatically configure remote debugging to connect to the kernel daemon. This must be supported regardless of whether the kernel is running on the same machine as the client or is running on a different machine that is reachable via TCP.
- Scripts can run in parallel.

### Script Dependency Management

- The IDE must have a way of configuring which Bowler libraries are dev'd, if any.
  - The user must be able to dev a library by selecting the location of a library's source code.
  - The user must be able to dev a library by selecting the Git Repo URI of a library. The source code of the library should be put into a directory on disk.
  - The user must be able to remove a dev.
  - Adding a dev must add that source code folder to their project. ! RFC: Do we want this? It seems overkill because I expect that only intermediate to advanced users will dev things.

### Robot Configuration Editor

- Robot configuration files must have a unique extension.
- Opening a robot configuration file shows a rich UI similar to the bundled Markdown editor (you can see raw text, a rich view, or both side-by-side).
  - Our rich view will support input, unlike the Markdown plugin.
- UI elements for:
  - Device type (e.g., ESP32).
  - Device resources (e.g., sensors, actuators).
    - Allocating a resource to a device must be validated. If a user puts a servo on a PWM pin, that is allowed. If a user puts a servo on an analog input pin, that is not allowed and must show an error in the UI.
  - Kinematics (e.g., bases, limbs, links, DH parameters, root offsets).
  - Scripts (e.g., body/limb/joint controllers, body/limb CAD generators).

### Plugins

- The IDE must be able to display a searchable list of plugins.
  - The IDE must display the group, name, and version triple.
  - The IDe must display the plugin artifact's description.
  - By default, the IDE displays all plugins in the group `com.commonwealthrobotics`.
  - The user may filter for a specific type of plugin (e.g. device, toolchain, device resource, or simulator).
- There is a list of default plugins which are installed by default.

### Git and GitHub Integration

- The IDE must present a simplified view of Git and GitHub to the user.
  - The user must be able to commit and push their changes from one simplified modal interface.
  - The user must be able to commit and push their changes in a dev to a fork from one simplified modal interface. ! RFC: Do we want this? It seems overkill because I expect that only intermediate to advanced users will dev things.

### Interface with the Kernel

- The kernel daemon must be started and connected to (if not already connected) when the user loads a project.
- If the kernel daemon becomes unresponsive, it must be restarted.
- If the project is closed, the kernel daemon must be killed.
  - There must be a graceful shutdown method that is tried first. If that fails, the process must be killed by the OS.
- If the kernel needs credentials, the IDE automatically gives the user's credentials.
  - The user can disable handling this automatically.

### Miscellaneous

- Single install: the user only installs an IntelliJ plugin. All other dependencies (including platform-specific artifacts) must be bundled inside the plugin Jar and self-extracted at runtime.
- Progress updates for long-running kernel operations.
  - For example, when the kernel starts cloning a repo, the user should see a progress bar that indicated when the clone is in progress.
- Warnings for poor Git behavior.
  - If the user has had uncommitted changes for longer than one week, they are shown a warning toast once that tells them they should commit their changes.

## Non-Functional Requirements

### Open Source

- All core components and their dependencies must be open-source under a compatible license. Some optional, non-core components may have closed-source dependencies.

### Portability

- x86_64 Windows, macOS, and Linux must be supported.

### Usability

- New users must be able to create a working robot without leaving our documentation.
- The user must not have to install anything other than the IntelliJ plugin.
