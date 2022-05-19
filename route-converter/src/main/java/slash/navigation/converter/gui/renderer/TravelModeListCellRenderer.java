/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui.renderer;

import java.awt.Component;
import java.util.MissingResourceException;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.gui.renderer.BackendListCellRenderer;
import slash.navigation.routing.TravelMode;

/**
 * Renders the {@link TravelMode} labels of the route travel mode combo box.
 *
 * @author Christian Pesch
 */

public class TravelModeListCellRenderer extends BackendListCellRenderer {
    public TravelModeListCellRenderer(ListCellRenderer backend) {
		super(backend);
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        TravelMode travelMode = (TravelMode) value;
        String text;
        try {
            text = RouteConverter.getBundle().getString("travel-mode-" + travelMode.getName().toLowerCase());
        } catch (MissingResourceException e) {
            text = travelMode.getName();
        }
        label.setText(text);
        return label;
    }
}
