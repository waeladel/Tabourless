package com.tabourless.queue.ui.report;

import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.tabourless.queue.data.ReportRepository;
import com.tabourless.queue.models.Message;
import com.tabourless.queue.models.User;

public class ReportViewModel extends ViewModel {

    private final static String TAG = ReportViewModel.class.getSimpleName();

    private ReportRepository mReportRepository;


    public ReportViewModel() {
        Log.d(TAG, "RevealViewModel init");
        mReportRepository = new ReportRepository();

    }

    public void sendReport(String userId, String currentUserId, User user, User currentUser, String issue) {
        // send profile report
        mReportRepository.sendReport(userId, currentUserId, user , currentUser, issue);
    }

    public void sendReport(String userId, String currentUserId, User user, User currentUser, String issue, Message message) {
        // send message report
        mReportRepository.sendReport(userId, currentUserId, user , currentUser, issue, message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "RevealViewModel onCleared:");
    }


}
