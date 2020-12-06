/*
 * This file is part of bowler-intellij.
 *
 * bowler-intellij is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * bowler-intellij is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with bowler-intellij.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.commonwealthrobotics.bowlerintellij.plugin;

import arrow.core.Tuple2;
import com.commonwealthrobotics.bowlerkernel.kerneldiscovery.NameClient;
import com.commonwealthrobotics.bowlerkernel.kerneldiscovery.NameServer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import java.net.InetAddress;
import java.nio.file.Paths;
import javax.annotation.CheckForNull;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shows the kernels reachable on this network. */
public class KernelDiscoveryForm {
  private final @NotNull KernelConnectionManager kernelConnectionManager;
  private JBList<Tuple2<String, InetAddress>> kernelsList;
  JPanel content;
  private JButton refreshButton;
  private JButton connectToSelectedKernelButton;
  private JLabel currentlyConnectedKernelLabel;
  private JButton disconnectFromCurrentKernelButton;
  private JTextField managedKernelServerNameField;
  private JButton startManagedKernelServerButton;
  private JButton stopManagedKernelServerButton;

  private final DefaultListModel<Tuple2<String, InetAddress>> kernelsListModel =
      new DefaultListModel<>();
  private KernelServerFacade kernelServerFacade;
  private static final Logger logger = LoggerFactory.getLogger(KernelDiscoveryForm.class);

  public KernelDiscoveryForm(
      @NotNull ToolWindow toolWindow, @NotNull KernelConnectionManager kernelConnectionManager) {
    this.kernelConnectionManager = kernelConnectionManager;

    kernelsList.setCellRenderer(new KernelListCellRenderer());
    kernelsList.setModel(kernelsListModel);

    refreshButton.addActionListener(e -> updateKernelsListModel());

    // Add a connection listener before we hook up the connect and disconnect buttons
    this.kernelConnectionManager.addConnectionListener(this::updateConnectedKernel);
    connectToSelectedKernelButton.addActionListener(e -> connectToKernel());
    disconnectFromCurrentKernelButton.addActionListener(e -> disconnectFromKernel());

    startManagedKernelServerButton.addActionListener(
        e -> startManagedKernelServer(managedKernelServerNameField.getText()));
    stopManagedKernelServerButton.addActionListener(e -> stopManagedKernelServer());

    // Do the initial update
    updateKernelsListModel();
  }

  private void startManagedKernelServer(String kernelName) {
    if (kernelServerFacade == null) {
      logger.debug("Starting kernel: {}", kernelName);
      // TODO: Expose the CLI path as a setting
      kernelServerFacade =
          new KernelServerFacade(
              kernelName,
              Paths.get(
                  "/home/salmon/Documents/bowler-kernel/cli/build/install/cli/bin/bowler-kernel"));
      kernelServerFacade.ensureStarted();
      delayedUpdateKernelsListModel(2000); // Update to show the new kernel
      setNewKernelServerUIState();
    } else {
      logger.debug(
          "Not starting a new kernel server with name {} because one is already running.",
          kernelName);
    }
  }

  private void stopManagedKernelServer() {
    if (kernelServerFacade == null) {
      logger.debug("Not stopping the kernel server because there is not one running.");
    } else {
      logger.debug("Stopping kernel");
      kernelServerFacade.ensureStopped();
      delayedUpdateKernelsListModel(1000); // Update to stop showing the kernel
      kernelServerFacade = null;
      setNewKernelServerUIState();
    }
  }

  private void connectToKernel() {
    final var selectedKernel = kernelsList.getSelectedValue();
    if (selectedKernel == null) {
      return; // Nothing to connect to
    }

    logger.debug(
        "Connecting to kernel {}:{}",
        selectedKernel.getA(),
        selectedKernel.getB().getHostAddress());

    // TODO: Expose these arguments
    final var port =
        NameClient.INSTANCE
            .getGrpcPort(selectedKernel.getB(), NameServer.defaultPort, 1000, 10)
            .attempt()
            .unsafeRunSync();

    port.fold(
        ex -> {
          logger.debug("Failed to get the kernel port.", ex);
          currentlyConnectedKernelLabel.setText("Error connecting.");
          return null;
        },
        p -> {
          logger.debug(
              "Connected to kernel {}:{} on port {}",
              selectedKernel.getA(),
              selectedKernel.getB().getHostAddress(),
              p);
          kernelConnectionManager.connect(selectedKernel.getB(), p);
          currentlyConnectedKernelLabel.setText(selectedKernel.getB().getHostAddress() + ":" + p);
          setSelectedKernelUIState();
          return null;
        });
  }

  private void disconnectFromKernel() {
    kernelConnectionManager.disconnect();
    setSelectedKernelUIState();
  }

  private Unit updateConnectedKernel(@CheckForNull Tuple2<? extends InetAddress, Integer> kernel) {
    if (kernel == null) {
      currentlyConnectedKernelLabel.setText(BIB.INSTANCE.message("kernel.discovery.not.connected"));
    } else {
      currentlyConnectedKernelLabel.setText(kernel.getA().toString() + ":" + kernel.getB());
    }
    return Unit.INSTANCE;
  }

  private void updateKernelsListModel() {
    // TODO: Expose these arguments
    var kernels =
        NameClient.INSTANCE.scan(
            NameServer.Companion.getDefaultMulticastGroup(), NameServer.defaultPort, 1000);
    kernelsListModel.clear();
    kernelsListModel.addAll(kernels);
  }

  private void delayedUpdateKernelsListModel(long delayMs) {
    new Thread(
            () -> {
              try {
                Thread.sleep(delayMs);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
              updateKernelsListModel();
            })
        .start();
  }

  private void setSelectedKernelUIState() {
    connectToSelectedKernelButton.setEnabled(!kernelConnectionManager.isConnected());
    disconnectFromCurrentKernelButton.setEnabled(kernelConnectionManager.isConnected());
  }

  private void setNewKernelServerUIState() {
    var kernelServerStarted = kernelServerFacade != null && kernelServerFacade.isStarted();
    startManagedKernelServerButton.setEnabled(!kernelServerStarted);
    stopManagedKernelServerButton.setEnabled(kernelServerStarted);
    managedKernelServerNameField.setEnabled(!kernelServerStarted);
  }
}
