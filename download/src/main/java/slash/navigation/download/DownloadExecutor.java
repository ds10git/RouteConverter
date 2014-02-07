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

package slash.navigation.download;

import slash.common.type.CompactCalendar;
import slash.navigation.download.actions.Copier;
import slash.navigation.download.actions.CopierListener;
import slash.navigation.download.actions.Extractor;
import slash.navigation.download.actions.Validator;
import slash.navigation.rest.Get;
import slash.navigation.rest.Head;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;
import static java.util.logging.Logger.getLogger;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.common.type.CompactCalendar.now;
import static slash.navigation.download.State.*;

/**
 * Performs the {@link Download} of an URL to local file.
 *
 * @author Christian Pesch
 */

public class DownloadExecutor implements Runnable {
    private static final Logger log = getLogger(DownloadExecutor.class.getName());

    private Download download;
    private DownloadTableModel model;

    public DownloadExecutor(Download download, DownloadTableModel model) {
        this.download = download;
        this.model = model;
    }

    public Download getDownload() {
        return download;
    }

    public void run() {
        try {
            boolean success = true;
            if (head())
                success = resume();

            boolean tempFileComplete = download.getContentLength().equals(download.getTempFile().length());
            if (!tempFileComplete)
                success = download();

            if (success) {
                State result = Succeeded;

                Validator validator = new Validator(download.getTempFile());
                if(!validator.existsFile())
                    result = NoFileError;
                else if(!validator.validSize(download.getSize()))
                    result = SizeError;
                else if(!validator.validChecksum(download.getChecksum()))
                    result = ChecksumError;

                postProcess();
                updateState(download, result);
                return;
            }
        } catch (Exception e) {
            log.severe(format("Could not download content from %s: %s", download.getUrl(), e.getMessage()));
        }
        updateState(download, Failed);

        // finally set date of latest sync
        download.setLastSync(now());
    }

    private String formatTime(CompactCalendar calendar) {
        return calendar != null ? DateFormat.getDateTimeInstance(SHORT, MEDIUM).format(calendar.getTime()) : "<none>";
    }

    private boolean head() throws IOException {
        Long tempLastModified = download.getTempFile().exists() ? download.getTempFile().lastModified() : 0;

        Head head = new Head(download.getUrl());
        head.setIfModifiedSince(tempLastModified);
        head.execute(true);
        if (!head.isOk()) {
            log.warning(format("HEAD request for %s failed, need to download", download.getUrl()));
            return false;
        }

        CompactCalendar lastModified = head.getLastModified() != null ? fromMillis(head.getLastModified()) : null;

        boolean contentLengthEquals = download.getContentLength() != null && download.getContentLength().equals(head.getContentLength());
        boolean lastModifiedAfter = download.getLastModified() != null && (download.getLastModified().equals(lastModified) || download.getLastModified().after(lastModified));
        boolean tempFileComplete = download.getContentLength() != null && download.getContentLength().equals(download.getTempFile().length());
        if (contentLengthEquals && lastModifiedAfter && !tempFileComplete && head.getAcceptByteRanges())
            return true;

        if (!contentLengthEquals)
            log.warning("HEAD content length is " + head.getContentLength() + " but download started with " + download.getContentLength() + " bytes, need to download");
        download.setContentLength(head.getContentLength());

        if (!lastModifiedAfter)
            log.warning("HEAD last modified " + formatTime(lastModified) + " is later than " + formatTime(download.getLastModified()) + " when download started, need to download");
        download.setLastModified(lastModified);

        return false;
    }

    private void updateState(Download download, State state) {
        download.setState(state);
        model.updateDownload(download);
    }


    private class ModelUpdater implements CopierListener {
        public void expectingBytes(long byteCount) {
            download.setExpectedBytes(byteCount);
        }

        public void processedBytes(long byteCount) {
            download.setProcessedBytes(byteCount);
            model.updateDownload(download);
        }
    }

    private ModelUpdater modelUpdater = new ModelUpdater();

    private boolean resume() throws IOException {
        updateState(download, Resuming);
        long fileSize = download.getTempFile().length();
        Long contentLength = download.getContentLength();
        log.severe(format("Resuming bytes %d-%d from %s", fileSize, contentLength, download.getUrl()));

        Get get = new Get(download.getUrl());
        get.setRange(fileSize, contentLength);
        InputStream inputStream = get.executeAsStream(true);
        if (get.isSuccessful() && get.isPartialContent()) {
            modelUpdater.expectingBytes(contentLength);
            new Copier(modelUpdater).copyAndClose(inputStream, new FileOutputStream(download.getTempFile(), true), fileSize, contentLength);
            return true;
        }

        return false;
    }

    private boolean download() throws IOException {
        updateState(download, Downloading);
        Long contentLength = download.getContentLength();
        log.severe(format("Downloading %d bytes from %s", contentLength, download.getUrl()));

        Get get = new Get(download.getUrl());
        InputStream inputStream = get.executeAsStream(true);
        if (get.isSuccessful()) {
            modelUpdater.expectingBytes(contentLength);
            new Copier(modelUpdater).copyAndClose(inputStream, new FileOutputStream(download.getTempFile()), 0, contentLength);
            return true;
        }
        return false;
    }

    private void postProcess() throws IOException {
        updateState(download, Processing);

        Action action = download.getAction();
        switch (action) {
            case Copy:
                new Copier(modelUpdater).copyAndClose(new FileInputStream(download.getTempFile()), new FileOutputStream(download.getTarget()), 0, download.getTempFile().length());
                break;
            case Extract:
                new Extractor(modelUpdater).extract(download.getTempFile(), download.getTarget());
                break;
            default:
                throw new IllegalArgumentException("Unknown Action " + action);
        }

        if (!download.getTempFile().delete())
            throw new IOException(format("Cannot delete temp file %s", download.getTempFile()));
    }
}
