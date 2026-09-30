package com.madhankurukondar.cardkeep;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ContactStore {
    private static final String PREFS = "cardkeep_data";
    private static final String KEY = "contacts";

    private ContactStore() {}

    public static List<ContactRecord> load(Context context) {
        List<ContactRecord> out = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                out.add(ContactRecord.fromJson(a.getJSONObject(i)));
            }
        } catch (Exception ignored) {}
        out.sort(Comparator.comparingLong((ContactRecord r) -> r.id).reversed());
        return out;
    }

    public static void save(Context context, List<ContactRecord> contacts) {
        JSONArray a = new JSONArray();
        for (ContactRecord r : contacts) a.put(r.toJson());
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, a.toString())
                .apply();
    }

    public static void upsert(Context context, List<ContactRecord> contacts, ContactRecord record) {
        int existing = -1;
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).id == record.id) {
                existing = i;
                break;
            }
        }
        if (existing >= 0) contacts.set(existing, record);
        else contacts.add(0, record);
        contacts.sort(Comparator.comparingLong((ContactRecord r) -> r.id).reversed());
        save(context, contacts);
    }
}
