package com.tcl.downloader.sample.support.sdk.bean;

import org.aisen.android.support.bean.ResultBean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by wangdan on 16/5/13.
 */
public class AppBeans extends ResultBean implements Serializable {

    private static final long serialVersionUID = 4782342771623804651L;

    private int resultStatus = -1;
    private int pages = 0;
    private int total = 0;
    private int page = 0;
    private String title;
    private List<AppBean> items;

    public int getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(int resultStatus) {
        this.resultStatus = resultStatus;
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<AppBean> getItems() {
        return items;
    }

    public void setItems(List<AppBean> items) {
        this.items = items;
    }
}
