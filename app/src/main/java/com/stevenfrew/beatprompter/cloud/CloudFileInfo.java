package com.stevenfrew.beatprompter.cloud;

import org.w3c.dom.Element;

import java.util.Date;

public class CloudFileInfo extends CloudItemInfo
{
    private final static String CLOUD_FILE_NAME_ATTRIBUTE_NAME="name";
    private final static String CLOUD_FILE_STORAGE_ID_ATTRIBUTE_NAME="storageID";
    private final static String CLOUD_FILE_LAST_MODIFIED_ATTRIBUTE_NAME="lastModified";
    private final static String CLOUD_FILE_SUBFOLDER_ATTRIBUTE_NAME="subfolder";

    public Date mLastModified;
    // Name of the immediate subfolder that it comes from, for filtering purposes.
    public String mSubfolder;

    public CloudFileInfo(String id,String name,Date lastModified,String subfolder)
    {
        super(id,name);
        mLastModified=lastModified;
        mSubfolder=subfolder;
    }

    public CloudFileInfo(Element element)
    {
        this(element.getAttribute(CLOUD_FILE_STORAGE_ID_ATTRIBUTE_NAME),
                element.getAttribute(CLOUD_FILE_NAME_ATTRIBUTE_NAME),
                new Date(Long.parseLong(element.getAttribute(CLOUD_FILE_LAST_MODIFIED_ATTRIBUTE_NAME))),
                element.getAttribute(CLOUD_FILE_SUBFOLDER_ATTRIBUTE_NAME));

    }

    public void writeToXML(Element element)
    {
        element.setAttribute(CLOUD_FILE_NAME_ATTRIBUTE_NAME,mName);
        element.setAttribute(CLOUD_FILE_STORAGE_ID_ATTRIBUTE_NAME,mID);
        element.setAttribute(CLOUD_FILE_LAST_MODIFIED_ATTRIBUTE_NAME,""+mLastModified.getTime());
        element.setAttribute(CLOUD_FILE_SUBFOLDER_ATTRIBUTE_NAME,mSubfolder==null?"":mSubfolder);
    }
}
