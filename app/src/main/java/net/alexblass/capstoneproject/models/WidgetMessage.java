package net.alexblass.capstoneproject.models;

/**
 * Data model to store widget message information.
 */

public class WidgetMessage {
    private String mKey;
    private String mSender;
    private String mDate;
    private boolean mIsRead;

    public WidgetMessage(String key, String sender, String date, boolean isRead){
        this.mKey = key;
        this.mSender = sender;
        this.mDate = date;
        this.mIsRead = isRead;
    }

    public String getKey() {
        return mKey;
    }

    public String getSender() {
        return mSender;
    }

    public void setSender(String sender) {
        this.mSender = sender;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public boolean isIsRead() {
        return mIsRead;
    }

    public void setIsRead(boolean isRead) {
        this.mIsRead = isRead;
    }
}
