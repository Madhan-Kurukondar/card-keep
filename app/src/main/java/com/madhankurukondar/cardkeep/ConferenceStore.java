package com.madhankurukondar.cardkeep;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ConferenceStore {
    private static final String PREFS = "cardkeep_conferences";
    private static final String KEY = "conferences";

    private ConferenceStore() {}

    public static List<ConferenceRecord> load(Context context) {
        List<ConferenceRecord> out = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                out.add(ConferenceRecord.fromJson(a.getJSONObject(i)));
            }
        } catch (Exception ignored) {}
        out.sort(Comparator.comparingLong((ConferenceRecord r) -> r.createdAt).reversed());
        return out;
    }

    public static void save(Context context, List<ConferenceRecord> conferences) {
        JSONArray a = new JSONArray();
        for (ConferenceRecord r : conferences) a.put(r.toJson());
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, a.toString())
                .apply();
    }

    public static void upsert(Context context, List<ConferenceRecord> conferences, ConferenceRecord record) {
        int existing = -1;
        for (int i = 0; i < conferences.size(); i++) {
            if (conferences.get(i).id.equals(record.id)) {
                existing = i;
                break;
            }
        }
        if (existing >= 0) conferences.set(existing, record);
        else conferences.add(0, record);
        conferences.sort(Comparator.comparingLong((ConferenceRecord r) -> r.createdAt).reversed());
        save(context, conferences);
    }
}
