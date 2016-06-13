package com.tcl.downloader.sample.support.sdk.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by wangdan on 16/5/13.
 */
public class AppBean implements Serializable {

    private static final long serialVersionUID = 4034491328096257391L;

    private long id;
    private String name;
    private String package_name;
    private String mVersionCode;
    private String version_name;
    private long size;
    private String publisher;
    private String mProvider;
    private String apk_url;
    private String icon_url;
    private int mRank;
    private long download_count;
    private long mAddTime;
    private long mUpdateTime;
    private String tag;
    private ArrayList<AppBeans> related_apps;
    private String url;
    private String mIconPath; // jipeng.sun
    private boolean mIconDownloadStatus; // jipeng.sun,ture->downloading
    private long mDownloadSize;
    private int mDownloadStatus = 6; // jipeng.sun
    private int mDownLoadPercent; // jipeng.sun
    private int mInstallStatus; // jipeng.sun
    private int mReadCount;
    private String mPayType;
    private String type;
    private int status;
    private int mRecommendCount;
    private String mBannerIcon;
    private float mPrice;
    private String mTpApk;
    private String mRealApkURL;// mTpApk is a link from third part, it must
    private String mTpIcon;
    private int mPosition;
    private String description;
    private String mEditRecommend;
    private float stars;
    private String[] mAppCategories;
    private int pages;
    private int total;
    private String sub_comment;
    private int scene_id;
    private String apkMd5;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackage_name() {
        return package_name;
    }

    public void setPackage_name(String package_name) {
        this.package_name = package_name;
    }

    public String getmVersionCode() {
        return mVersionCode;
    }

    public void setmVersionCode(String mVersionCode) {
        this.mVersionCode = mVersionCode;
    }

    public String getVersion_name() {
        return version_name;
    }

    public void setVersion_name(String version_name) {
        this.version_name = version_name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getmProvider() {
        return mProvider;
    }

    public void setmProvider(String mProvider) {
        this.mProvider = mProvider;
    }

    public String getApk_url() {
        return apk_url;
    }

    public void setApk_url(String apk_url) {
        this.apk_url = apk_url;
    }

    public String getIcon_url() {
        return icon_url;
    }

    public void setIcon_url(String icon_url) {
        this.icon_url = icon_url;
    }

    public int getmRank() {
        return mRank;
    }

    public void setmRank(int mRank) {
        this.mRank = mRank;
    }

    public long getDownload_count() {
        return download_count;
    }

    public void setDownload_count(long download_count) {
        this.download_count = download_count;
    }

    public long getmAddTime() {
        return mAddTime;
    }

    public void setmAddTime(long mAddTime) {
        this.mAddTime = mAddTime;
    }

    public long getmUpdateTime() {
        return mUpdateTime;
    }

    public void setmUpdateTime(long mUpdateTime) {
        this.mUpdateTime = mUpdateTime;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public ArrayList<AppBeans> getRelated_apps() {
        return related_apps;
    }

    public void setRelated_apps(ArrayList<AppBeans> related_apps) {
        this.related_apps = related_apps;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getmIconPath() {
        return mIconPath;
    }

    public void setmIconPath(String mIconPath) {
        this.mIconPath = mIconPath;
    }

    public boolean ismIconDownloadStatus() {
        return mIconDownloadStatus;
    }

    public void setmIconDownloadStatus(boolean mIconDownloadStatus) {
        this.mIconDownloadStatus = mIconDownloadStatus;
    }

    public long getmDownloadSize() {
        return mDownloadSize;
    }

    public void setmDownloadSize(long mDownloadSize) {
        this.mDownloadSize = mDownloadSize;
    }

    public int getmDownloadStatus() {
        return mDownloadStatus;
    }

    public void setmDownloadStatus(int mDownloadStatus) {
        this.mDownloadStatus = mDownloadStatus;
    }

    public int getmDownLoadPercent() {
        return mDownLoadPercent;
    }

    public void setmDownLoadPercent(int mDownLoadPercent) {
        this.mDownLoadPercent = mDownLoadPercent;
    }

    public int getmInstallStatus() {
        return mInstallStatus;
    }

    public void setmInstallStatus(int mInstallStatus) {
        this.mInstallStatus = mInstallStatus;
    }

    public int getmReadCount() {
        return mReadCount;
    }

    public void setmReadCount(int mReadCount) {
        this.mReadCount = mReadCount;
    }

    public String getmPayType() {
        return mPayType;
    }

    public void setmPayType(String mPayType) {
        this.mPayType = mPayType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getmRecommendCount() {
        return mRecommendCount;
    }

    public void setmRecommendCount(int mRecommendCount) {
        this.mRecommendCount = mRecommendCount;
    }

    public String getmBannerIcon() {
        return mBannerIcon;
    }

    public void setmBannerIcon(String mBannerIcon) {
        this.mBannerIcon = mBannerIcon;
    }

    public float getmPrice() {
        return mPrice;
    }

    public void setmPrice(float mPrice) {
        this.mPrice = mPrice;
    }

    public String getmTpApk() {
        return mTpApk;
    }

    public void setmTpApk(String mTpApk) {
        this.mTpApk = mTpApk;
    }

    public String getmRealApkURL() {
        return mRealApkURL;
    }

    public void setmRealApkURL(String mRealApkURL) {
        this.mRealApkURL = mRealApkURL;
    }

    public String getmTpIcon() {
        return mTpIcon;
    }

    public void setmTpIcon(String mTpIcon) {
        this.mTpIcon = mTpIcon;
    }

    public int getmPosition() {
        return mPosition;
    }

    public void setmPosition(int mPosition) {
        this.mPosition = mPosition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JSONField(name = "editor_recommend")
    public String getmEditRecommend() {
        return mEditRecommend;
    }

    @JSONField(name = "editor_recommend")
    public void setmEditRecommend(String mEditRecommend) {
        this.mEditRecommend = mEditRecommend;
    }

    public float getStars() {
        return stars;
    }

    public void setStars(float stars) {
        this.stars = stars;
    }

    public String[] getmAppCategories() {
        return mAppCategories;
    }

    public void setmAppCategories(String[] mAppCategories) {
        this.mAppCategories = mAppCategories;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getSub_comment() {
        return sub_comment;
    }

    public void setSub_comment(String sub_comment) {
        this.sub_comment = sub_comment;
    }

    public int getScene_id() {
        return scene_id;
    }

    public void setScene_id(int scene_id) {
        this.scene_id = scene_id;
    }

    public String getApkMd5() {
        return apkMd5;
    }

    public void setApkMd5(String apkMd5) {
        this.apkMd5 = apkMd5;
    }
}
