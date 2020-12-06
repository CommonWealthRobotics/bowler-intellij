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
import java.util.List;
import javax.annotation.CheckForNull;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shows the kernels reachable on this network. Lets you start a kernel managed by the plugin. */
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
      disableManagedKernelServerUI();
      new StartManagedKernelServer(kernelName).execute();
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
      disableManagedKernelServerUI();
      new StopManagedKernelServer();
    }
  }

  private void connectToKernel() {
    final var selectedKernel = kernelsList.getSelectedValue();
    if (selectedKernel == null) {
      return; // Nothing to connect to
    }

    disableSelectedKernelUI();
    new ConnectToKernel(selectedKernel).execute();
  }

  private void disconnectFromKernel() {
    disableSelectedKernelUI();
    new DisconnectFromKernel().execute();
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

  private void setManagedKernelServerUIState() {
    var kernelServerStarted = kernelServerFacade != null && kernelServerFacade.isStarted();
    startManagedKernelServerButton.setEnabled(!kernelServerStarted);
    stopManagedKernelServerButton.setEnabled(kernelServerStarted);
    managedKernelServerNameField.setEnabled(!kernelServerStarted);
  }

  private void disableManagedKernelServerUI() {
    startManagedKernelServerButton.setEnabled(false);
    stopManagedKernelServerButton.setEnabled(false);
    managedKernelServerNameField.setEnabled(false);
  }

  private void setSelectedKernelUIState() {
    connectToSelectedKernelButton.setEnabled(!kernelConnectionManager.isConnected());
    disconnectFromCurrentKernelButton.setEnabled(kernelConnectionManager.isConnected());
  }

  private void disableSelectedKernelUI() {
    connectToSelectedKernelButton.setEnabled(false);
    disconnectFromCurrentKernelButton.setEnabled(false);
  }

  private class UpdateKernelsList
      extends SwingWorker<List<Tuple2<String, InetAddress>>, Tuple2<String, InetAddress>> {

    private final long delayMs;

    public UpdateKernelsList(long delayMs) {
      this.delayMs = delayMs;
    }

    @Override
    protected List<Tuple2<String, InetAddress>> doInBackground() throws Exception {
      Thread.sleep(delayMs);
      // TODO: Expose these arguments
      return NameClient.INSTANCE.scan(
          NameServer.Companion.getDefaultMulticastGroup(), NameServer.defaultPort, 1000);
    }

    @Override
    protected void done() {
      kernelsListModel.clear();
      try {
        kernelsListModel.addAll(get());
      } catch (Exception ignore) {
      }
    }
  }

  private class StartManagedKernelServer extends SwingWorker<Void, Void> {
    private final String kernelName;

    public StartManagedKernelServer(String kernelName) {
      this.kernelName = kernelName;
    }

    @Override
    protected Void doInBackground() {
      logger.debug("Starting a new managed kernel with name {}", kernelName);

      // TODO: Expose the CLI path as a setting
      kernelServerFacade =
          new KernelServerFacade(
              kernelName,
              Paths.get(
                  "/home/salmon/Documents/bowler-kernel/cli/build/install/cli/bin/bowler-kernel"));
      kernelServerFacade.ensureStarted();

      var updateKernelsWorker = new UpdateKernelsList(2000);
      updateKernelsWorker.execute();

      return null;
    }

    @Override
    protected void done() {
      setManagedKernelServerUIState();
    }
  }

  private class StopManagedKernelServer extends SwingWorker<Void, Void> {

    @Override
    protected Void doInBackground() {
      logger.debug("Stopping managed kernel");
      kernelServerFacade.ensureStopped();

      var updateKernelsWorker = new UpdateKernelsList(1000);
      updateKernelsWorker.execute();

      kernelServerFacade = null;

      return null;
    }

    @Override
    protected void done() {
      setManagedKernelServerUIState();
    }
  }

  private class ConnectToKernel extends SwingWorker<Tuple2<InetAddress, Integer>, Void> {
    private final Tuple2<String, InetAddress> selectedKernel;

    public ConnectToKernel(Tuple2<String, InetAddress> selectedKernel) {
      this.selectedKernel = selectedKernel;
    }

    @Override
    protected Tuple2<InetAddress, Integer> doInBackground() {
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

      return port.fold(
          ex -> {
            logger.debug("Failed to get the kernel port.", ex);
            return null;
          },
          p -> {
            logger.debug(
                "Connected to kernel {}:{} on port {}",
                selectedKernel.getA(),
                selectedKernel.getB().getHostAddress(),
                p);
            kernelConnectionManager.connect(selectedKernel.getB(), p);
            return new Tuple2<>(selectedKernel.getB(), p);
          });
    }

    @Override
    protected void done() {
      try {
        var result = get();
        if (result == null) {
          currentlyConnectedKernelLabel.setText("Error connecting.");
        } else {
          currentlyConnectedKernelLabel.setText(
              result.getA().getHostAddress() + ":" + result.getB());
          setSelectedKernelUIState();
        }
      } catch (Exception ignore) {
      }
    }
  }

  private class DisconnectFromKernel extends SwingWorker<Void, Void> {
    @Override
    protected Void doInBackground() {
      kernelConnectionManager.disconnect();
      return null;
    }

    @Override
    protected void done() {
      setSelectedKernelUIState();
    }
  }
}
