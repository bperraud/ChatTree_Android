package com.chattree.chattree.home.conversation;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class ConversationItem {

    private int id;
    private String title;
    private String picture;
    private List<String> memberLabels;

    public ConversationItem(int id, String title, String picture) {
        this.id = id;
        this.title = title;
        this.picture = picture;
        this.memberLabels = new ArrayList<>();
    }

    public void addMemberLabel(String label) {
        memberLabels.add(label);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPicture() {
        return picture;
    }

    public List<String> getMemberLabels() {
        return memberLabels;
    }

    public String getMemberLabelsFormated() {
        return TextUtils.join(", ", memberLabels);
    }
}
