# Plugin Design

## Functional Requirements

### Create a Project

- Users can create a new project using IntelliJ's project creation dialog.
  - Projects can be libraries (no robot config file) or robots (one robot config file).
  - A new Git repo will be initialized in the project directory.
    - A gitignore file will be added that has the relevant entries for our software, IDEs (e.g., Intellij, Eclipse, vscode), and common technologes (e.g., Java, Gradle).
      - The directory for persistent storage for scripts is gitignore'd by default.

### Launch Robots

- Users can create a run configuration that loads a robot via its config file and a connection method.
  - Users can add dev'd dependencies by repo or local file path (only supported for dependencies on other library projects).
    - Adding via a repo should clone the repo into a local cache and then always resolve to that cache. Users can edit the repo inside the cache.
    - Adding via a local file path simply resolves to that path. Users can edit the files under that path.
  - Dev'ing a Maven dependency is not officially supported.
    - ! RFC: Maven local should work with Grapes if the kernel runs on the same machine as the Maven local repository. We should investigate this. If it works, then we should officially support this case (and still not support this if the kernel is running on a different machine than the Maven local repository).
  - Users can choose whether the kernel should connect to real hardware or to a simulator.
    - If hardware is selected, the user must supply a connection method (e.g., USB HID, UDP).
    - If a simulator is selected, the user must select which simulator implementation (e.g., Bullet, PhysX). The IDE must ask the kernel what simulator plugins are available.
- One or more connection methods may be specified. If more than once connection method is specified, then each method is tried in order. If one method fails, the next method in the list is tried. If all methods fail, an error is thrown.

### Run Scripts

- Users can create a run configuration that runs a script (this is just the default Groovy run configuration).
  - If the user runs in the debug configuration, the IDE should automatically configure remote debugging to connect to the kernel daemon. This must be supported regardless of whether the kernel is running on the same machine as the client or is running on a different machine that is reachable via TCP.
    - This will involve starting a kernel daemon that is configured for remote debugging. You cannot take a kernel that was launched without remote debugging and add remote debugging capabilities to it at runtime.
    - This will also involve asking the kernel what its JDWB port is, after it is running. There is a kernel gRPC call for that.
- Scripts can run in parallel.

### Script Dependency Management

- The IDE must have a way of configuring which Bowler libraries are dev'd, if any.
  - The user must be able to dev a library by selecting the location of a library's source code.
  - The user must be able to dev a library by selecting the Git Repo URI of a library. The source code of the library should be put into a directory on disk.
  - The user must be able to remove a dev.

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
  - Computing center of mass from vitamins (and optionally, from generated CAD as well)

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

### Interface with the Kernel

- When transferring the project to the kernel, the remote `origin` will be used. The patch will be computed relative to the `HEAD` commit on that remote.
- The kernel daemon must be started and connected to (if not already connected) when the user loads a project.
  - When the kernel is connected, the plugin should load the display daemon and connect the kernel to it.
- If the kernel daemon becomes unresponsive, it must be restarted.
- If the project is closed, the kernel and display daemons must be killed.
  - There must be a graceful shutdown method that is tried first. If that fails, the process must be killed by the OS.
- If the kernel needs credentials, the IDE prompts the user for their credentials.
  - The user is first presented with a modal dialog that asks them to enter their credentials. The user also has options to control how the credentials are remembered (e.g. `yes`, `no`, `only for this session`). The user may `submit` or `cancel` their submission of their credentials.
  - If the user's credentials worked and they selected `yes`, then the IDE should automatically send their credentials to the kernel if it asks for them and should persist the credentials between IDE sessions.
  - If the user's credentials worked and they selected `only for this session`, then the IDE should automatically send their credentials to the kernel if it asks for them and should clear the credentials once the user closes the IDE.
  - If the user's credentials worked and they selected `no`, then the IDE should not persist their credentials and should prompt the user for their credentials if the kernel asks for them again.
  - If the user's credentials did not work, the IDE should ask the user to enter their credentials again.
  - If the user selected `cancel` instead of `submit`, then the IDE should respond to the kernel that its credentials request was denied. This will most likely cause the script to fail.

### Installing and Updating

- Installing and updating the plugin is handled through the plugin marketplace
- The plugin needs to be able to download ZGC and check for ZGC updates.
- The plugin needs to be able to check for kernel and display updates
  - If an update is available, then the plugin must ask the user if they want to update. This modal dialog should present the current and new versions to the user.
  - If the kernel is not remote, then it needs to download the new version and give the file path to the kernel so that it can update and restart itself.
    - This workflow should also be used for updating the display. The kernel and the display should use the same updater bootstrap library.
  - If the kernel is remote, then it needs to use the gRPC call to send the Jar over to the embedded computer and have it update and restart the kernel.
- Single install: the user only installs an IntelliJ plugin. All other dependencies (like the kernel and display Jars and the JVM) must be downloaded and installed using the normal updating machinery.

### Miscellaneous

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
