/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

/**
 * The download manager API triggers strict mode exception in UI thread.
 */
class DownloadManagerRequestTask extends AsyncTask<String, Void, Void> {

    private final DownloadManagerReleaseDownloader mDownloader;

    DownloadManagerRequestTask(DownloadManagerReleaseDownloader downloader) {
        mDownloader = downloader;
    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            /* Download file. */
            ReleaseDetails releaseDetails = mDownloader.getReleaseDetails();
            Uri downloadUrl = releaseDetails.getDownloadUrl();
            AppCenterLog.debug(LOG_TAG, "Start downloading new release from " + downloadUrl);
            DownloadManager downloadManager = mDownloader.getDownloadManager();
            DownloadManager.Request request = createRequest(downloadUrl);
            request.setTitle(String.format(params[0], releaseDetails.getShortVersion(), releaseDetails.getVersion()));

            /* Hide mandatory download to prevent canceling via notification cancel or download UI delete. */
            if (releaseDetails.isMandatoryUpdate()) {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
                request.setVisibleInDownloadsUi(false);
            }
            long enqueueTime = System.currentTimeMillis();

            long downloadId = downloadManager.enqueue(request);
            if (!isCancelled()) {
                mDownloader.onDownloadStarted(downloadId, enqueueTime);
            }
            return null;
        } catch (Exception e) {

            /*
             * In cases when Download Manager application is disabled,
             * IllegalArgumentException: Unknown URL content://downloads/my_download is thrown.
             */
            mDownloader.onDownloadError(new IllegalStateException("Failed to start download: Download Manager is disabled.", e));
            return null;
        }
    }

    @VisibleForTesting
    DownloadManager.Request createRequest(Uri Uri) {
        return new DownloadManager.Request(Uri);
    }
}
