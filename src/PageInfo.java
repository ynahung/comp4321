package src;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class PageInfo implements Serializable {
    private Date date;
    private ArrayList<String> childUrls;
    private ArrayList<String> parentUrls;

    public PageInfo(Date date, ArrayList<String> pages, boolean isChild) {
        this.date = date;
        if (isChild) {
            this.childUrls = pages;
        } else {
            this.parentUrls = pages; // Use a flag to differentiate between child and parent
                                     // lists
        }
    }

    public Date getDate() {
        return date;
    }

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
