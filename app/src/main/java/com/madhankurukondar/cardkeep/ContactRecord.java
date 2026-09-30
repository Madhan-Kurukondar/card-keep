package com.madhankurukondar.cardkeep;

import org.json.JSONObject;

public class ContactRecord {
    public long id = System.currentTimeMillis();
    public String name = "";
    public String company = "";
    public String title = "";
    public String email = "";
    public String phone = "";
    public String mobile = "";
    public String website = "";
    public String address = "";

    public String conferenceId = "";
    public String event = "";
    public String metDate = "";
    public String location = "";
    public String howMet = "";
    public String relevance = "";
    public String discussion = "";
    public String opportunity = "";
    public String nextAction = "";
    public String followUp = "";
    public String status = "New";
    public String priority = "Normal";
    public String tags = "";
    public String notes = "";

    public String imagePath = "";
    public String rawText = "";

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("company", company);
            o.put("title", title);
            o.put("email", email);
            o.put("phone", phone);
            o.put("mobile", mobile);
            o.put("website", website);
            o.put("address", address);
            o.put("conferenceId", conferenceId);
            o.put("event", event);
            o.put("metDate", metDate);
            o.put("location", location);
            o.put("howMet", howMet);
            o.put("relevance", relevance);
            o.put("discussion", discussion);
            o.put("opportunity", opportunity);
            o.put("nextAction", nextAction);
            o.put("followUp", followUp);
            o.put("status", status);
            o.put("priority", priority);
            o.put("tags", tags);
            o.put("notes", notes);
            o.put("imagePath", imagePath);
            o.put("rawText", rawText);
        } catch (Exception ignored) {}
        return o;
    }

    public static ContactRecord fromJson(JSONObject o) {
        ContactRecord r = new ContactRecord();
        r.id = o.optLong("id", System.currentTimeMillis());
        r.name = o.optString("name", "");
        r.company = o.optString("company", "");
        r.title = o.optString("title", "");
        r.email = o.optString("email", "");
        r.phone = o.optString("phone", "");
        r.mobile = o.optString("mobile", "");
        r.website = o.optString("website", "");
        r.address = o.optString("address", "");
        r.conferenceId = o.optString("conferenceId", "");
        r.event = o.optString("event", "");
        r.metDate = o.optString("metDate", "");
        r.location = o.optString("location", "");
        r.howMet = o.optString("howMet", "");
        r.relevance = o.optString("relevance", "");
        r.discussion = o.optString("discussion", "");
        r.opportunity = o.optString("opportunity", "");
        r.nextAction = o.optString("nextAction", "");
        r.followUp = o.optString("followUp", "");
        r.status = o.optString("status", "New");
        r.priority = o.optString("priority", "Normal");
        r.tags = o.optString("tags", "");
        r.notes = o.optString("notes", "");
        r.imagePath = o.optString("imagePath", "");
        r.rawText = o.optString("rawText", "");
        return r;
    }
}
