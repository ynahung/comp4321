package src;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;

public class PageInfo implements Serializable {
    private Date date;
    private ArrayList<String> childUrls;
    private ArrayList<String> parentUrls;
    private long size;
    private String pageTitle;

    public PageInfo(Date date, Long size, ArrayList<String> pages, boolean isChild, String pageTitle) {
        this.date = date;
        this.size = size;
        if (isChild) {
            this.childUrls = pages;
        } else {
            this.parentUrls = pages; // Use a flag to differentiate between child and parent
                                     // lists
        }
        this.pageTitle = pageTitle;
    }

    public Date getDate() {
        return date;
    }

    public long getSize() { return size;}

    public String getPageTitle(){ return pageTitle;}

    public ArrayList<String> getchildUrls() {
        if (childUrls == null) {
            childUrls = new ArrayList<String>();
        }
        return childUrls;
    }

    public ArrayList<String> getParentUrls() {
        if (parentUrls == null) {
            parentUrls = new ArrayList<String>();
        }
        return parentUrls;
    }

    public void addChildPage(String page) {
        if (childUrls == null) {
            childUrls = new ArrayList<String>();
        }
        childUrls.add(page);
    }

    public void addParentUrl(String page) {
        if (parentUrls == null) {
            parentUrls = new ArrayList<String>();
        }
        parentUrls.add(page);
    }
}
