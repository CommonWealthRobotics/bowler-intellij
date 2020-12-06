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
import java.awt.Component;
import java.net.InetAddress;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/** Renders an element from a kernel discovery scan. Shows the kernel name and IP. */
class KernelListCellRenderer extends JLabel
    implements ListCellRenderer<Tuple2<String, InetAddress>> {

  public KernelListCellRenderer() {
    super();
    setOpaque(true);
  }

  @Override
  public Component getListCellRendererComponent(
      JList<? extends Tuple2<String, InetAddress>> list,
      Tuple2<String, InetAddress> value,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {
    if (value == null) {
      setText("");
    } else {
      setText(value.getA() + ":" + value.getB().getHostAddress());
    }

    setEnabled(list.isEnabled());
    setFont(list.getFont());

    if (isSelected) {
      setBackground(list.getSelectionBackground());
      setForeground(list.getSelectionForeground());
    } else {
      setBackground(list.getBackground());
      setForeground(list.getForeground());
    }

    return this;
  }
}
