package com.madhankurukondar.cardkeep;

import org.json.JSONObject;

public class ConferenceRecord {
    public String id = "conference_" + System.currentTimeMillis();
    public String name = "";
    public String location = "";
    public String startDate = "";
    public String endDate = "";
    public String defaultHowMet = "Conference / Trade Fair";
    public String defaultTags = "";
    public String notes = "";
    public long createdAt = System.currentTimeMillis();

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("location", location);
            o.put("startDate", startDate);
            o.put("endDate", endDate);
            o.put("defaultHowMet", defaultHowMet);
            o.put("defaultTags", defaultTags);
            o.put("notes", notes);
            o.put("createdAt", createdAt);
        } catch (Exception ignored) {}
        return o;
    }

    public static ConferenceRecord fromJson(JSONObject o) {
        ConferenceRecord r = new ConferenceRecord();
        r.id = o.optString("id", "conference_" + System.currentTimeMillis());
        r.name = o.optString("name", "");
        r.location = o.optString("location", "");
        r.startDate = o.optString("startDate", "");
        r.endDate = o.optString("endDate", "");
        r.defaultHowMet = o.optString("defaultHowMet", "Conference / Trade Fair");
        r.defaultTags = o.optString("defaultTags", "");
        r.notes = o.optString("notes", "");
        r.createdAt = o.optLong("createdAt", System.currentTimeMillis());
        return r;
    }
}
