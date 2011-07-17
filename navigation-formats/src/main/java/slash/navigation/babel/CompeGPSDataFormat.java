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

package slash.navigation.babel;

import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.util.List;

import static slash.common.io.Transfer.isEmpty;

/**
 * Reads and writes CompeGPS Data (.trk) files.
 *
 * @author Christian Pesch
 */

// TODO might want to create 1 Read, 3 Write formats like for OziExplorer

public class CompeGPSDataFormat extends BabelFormat {
    public String getExtension() {
        return ".trk";
    }

    public String getName() {
        return "CompeGPS Data (*" + getExtension() + ")";
    }

    protected String getFormatName() {
        return "compegps";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected boolean isStreamingCapable() {
        return true;
    }

    protected boolean isValidRoute(GpxRoute route) {
        // clashes with some iGo8 .trk files
        List<GpxPosition> positions = route.getPositions();
        int count = 0;
        for (GpxPosition position : positions) {
            if (isEmpty(position.getLongitude()) && isEmpty(position.getLatitude()))
                count++;
        }
        return count != positions.size();
    }
}
