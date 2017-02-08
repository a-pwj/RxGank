package com.sunfusheng.gank.util.update;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.sunfusheng.gank.R;
import com.sunfusheng.gank.util.ToastUtil;
import com.sunfusheng.gank.util.AppUtil;
import com.sunfusheng.gank.util.dialog.CommonDialog;
import com.sunfusheng.gank.util.dialog.DownloadDialog;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadStatus;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

/**
 * Created by sunfusheng on 2017/2/4.
 */
public class UpdateHelper {

    private Activity mActivity;
    private int lastProgress = 0;
    private DownloadDialog mDialog;
    private Disposable mDisposable;

    private String fileName = "RxGank.apk";
    private String filePath;
    private String apkPathName;

    public UpdateHelper(Activity activity) {
        this.mActivity = activity;
        filePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
        apkPathName = filePath + File.separator + fileName;
    }

    public void unInit() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }

    public void dealWithVersion(final VersionEntity entity) {
        String content = entity.getChangelog() + "\n\n下载(V" + entity.getVersionShort() + ")替换当前版本(" + AppUtil.getVersionName() + ")?";
        new CommonDialog(mActivity).show(
                mActivity.getString(R.string.update_app),
                content,
                mActivity.getString(R.string.update_rightnow),
                mActivity.getString(R.string.update_no),
                (dialog, which) -> {
                    download(entity.getInstall_url());
                });
    }

    public void download(String url) {
        RxPermissions rxPermissions = new RxPermissions(mActivity);
        rxPermissions.request(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnNext(granted -> {
                    if (!granted) {
                        ToastUtil.show("无权限向SDCard写数据");
                    }
                })
                .compose(RxDownload.getInstance().transform(url, fileName, filePath))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DownloadStatus>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                        mDialog = new DownloadDialog(mActivity);
                        mDialog.show();
                    }

                    @Override
                    public void onNext(DownloadStatus value) {
                        float progressF = (float) (value.getDownloadSize() * 1.0 / value.getTotalSize());
                        int progressI = (int) (progressF * 100);
                        if (progressI > lastProgress) {
                            lastProgress = progressI;
                            mDialog.setProgress(progressI);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mDialog.dismiss();
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        mDialog.dismiss();
                        installPackage(apkPathName);
                    }
                });
    }

    // 安装应用
    private void installPackage(String apkPathName) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(apkPathName)), "application/vnd.android.package-archive");
        mActivity.startActivity(intent);
    }
}
