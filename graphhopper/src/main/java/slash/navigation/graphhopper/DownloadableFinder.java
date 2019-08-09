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
package slash.navigation.graphhopper;

import slash.navigation.common.BoundingBox;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static slash.navigation.common.Bearing.calculateBearing;

/**
 * Finds {@link Downloadable}s for {@link BoundingBox}es from a given {@link DataSource}
 * and prefers {@link Downloadable}s that are already present.
 *
 * @author Christian Pesch
 */

public class DownloadableFinder {
    private static final Logger log = Logger.getLogger(DownloadableFinder.class.getName());
    private DataSource dataSource;
    private java.io.File directory;

    public DownloadableFinder(DataSource dataSource, java.io.File directory) {
        this.dataSource = dataSource;
        this.directory = directory;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private boolean existsFile(File file) {
        return file != null && new java.io.File(directory, file.getUri()).exists();
    }

    public List<Downloadable> getDownloadablesFor(BoundingBox routeBoundingBox) {
        List<DownloadableDescriptor> descriptors = new ArrayList<>();
        for (File file : dataSource.getFiles()) {
            BoundingBox fileBoundingBox = file.getBoundingBox();
            if (fileBoundingBox == null) {
                log.warning(format("File %s doesn't have a bounding box. Ignoring it.", file));
                continue;
            }
            if (!fileBoundingBox.contains(routeBoundingBox))
                continue;

            Double distance = calculateBearing(fileBoundingBox.getCenter().getLongitude(), fileBoundingBox.getCenter().getLatitude(),
                    routeBoundingBox.getCenter().getLongitude(), routeBoundingBox.getCenter().getLatitude()).getDistance();
            boolean existsFile = existsFile(file);
            descriptors.add(new DownloadableDescriptor(file, distance, fileBoundingBox, existsFile));
        }
        descriptors.sort(null);

        List<Downloadable> result = descriptors.stream()
                .map(DownloadableDescriptor::getDownloadable)
                .collect(Collectors.toList());
        log.info(format("Found %d downloadables for %s: %s", result.size(), routeBoundingBox, result));
        return result;
    }

    public Collection<Downloadable> getDownloadablesFor(List<BoundingBox> boundingBoxes) {
        Collection<Downloadable> result = new HashSet<>();
        for (BoundingBox boundingBox : boundingBoxes)
            result.addAll(getDownloadablesFor(boundingBox));
        return result;
    }

    private static class DownloadableDescriptor implements Comparable<DownloadableDescriptor> {
        private final Downloadable downloadable;
        private final Double distanceToCenter;
        private final BoundingBox fileBoundingBox;
        private final boolean existsFile;

        private DownloadableDescriptor(Downloadable downloadable, Double distanceToCenter, BoundingBox fileBoundingBox, boolean existsFile) {
            this.downloadable = downloadable;
            this.distanceToCenter = distanceToCenter;
            this.fileBoundingBox = fileBoundingBox;
            this.existsFile = existsFile;
        }

        public Downloadable getDownloadable() {
            return downloadable;
        }

        /*  Compare two Downloadables by:
            A < B if A exists and B not
            B < A if B exists and A not
            if (both exist or don't exist)
                A < B if B.contains(A)
                B < A if A.contains(B)
                if (neither contain each other)
                    A compares to B by DistanceToCenter of Route
        */
        public int compareTo(DownloadableDescriptor other) {
            if(existsFile && !other.existsFile)
                return -1;
            if(other.existsFile && !existsFile)
                return -1;

            if (fileBoundingBox != null && other.fileBoundingBox != null) {
                if (fileBoundingBox.contains(other.fileBoundingBox))
                    return 2;
                else if (other.fileBoundingBox.contains(fileBoundingBox))
                    return -2;
            }
            return distanceToCenter.compareTo(other.distanceToCenter);
        }
    }
}