package com.madhankurukondar.cardkeep;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 100;
    private static final int REQ_GALLERY = 101;
    private static final int REQ_EXPORT_VCARD = 102;
    private static final int REQ_CAMERA_PERMISSION = 103;

    // ArivEmb visual system — presentation only.
    private static final int SILICON = Color.rgb(7, 10, 18);
    private static final int NAVY = Color.rgb(11, 18, 32);
    private static final int PANEL = Color.rgb(18, 27, 43);
    private static final int PANEL_2 = Color.rgb(22, 31, 49);
    private static final int MAGENTA = Color.rgb(216, 63, 181);
    private static final int MAGENTA_LIGHT = Color.rgb(240, 123, 213);
    private static final int MAGENTA_DEEP = Color.rgb(155, 27, 120);
    private static final int WHITE = Color.rgb(245, 247, 250);
    private static final int GREY = Color.rgb(168, 178, 193);
    private static final int CIRCUIT = Color.rgb(41, 52, 73);
    private static final int CYAN = Color.rgb(38, 198, 218);

    private final List<ContactRecord> contacts = new ArrayList<>();
    private final List<ConferenceRecord> conferences = new ArrayList<>();
    private final Map<String, EditText> editorFields = new LinkedHashMap<>();
    private final Map<String, EditText> conferenceFields = new LinkedHashMap<>();

    private Uri pendingCameraUri;
    private ContactRecord editingRecord;
    private ContactRecord pendingExportRecord;
    private LinearLayout contactListContainer;
    private EditText searchBox;
    private ConferenceRecord currentConference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(SILICON);
        getWindow().setNavigationBarColor(SILICON);
        contacts.addAll(ContactStore.load(this));
        conferences.addAll(ConferenceStore.load(this));
        showHome();
    }

    @Override
    public void onBackPressed() {
        if (editingRecord != null) {
            returnFromEditor();
        } else if (currentConference != null) {
            showConferenceHub();
        } else {
            super.onBackPressed();
        }
    }

    private void showHome() {
        editingRecord = null;
        currentConference = null;
        editorFields.clear();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SILICON);

        LinearLayout root = column();
        root.setPadding(dp(18), dp(20), dp(18), dp(36));
        root.setBackgroundColor(SILICON);
        scroll.addView(root);

        // Brand header
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(dp(2), dp(2), dp(2), dp(12));

        ImageView brandMark = new ImageView(this);
        brandMark.setImageResource(com.madhankurukondar.cardkeep.R.mipmap.ic_launcher);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(52), dp(52));
        markLp.setMargins(0, 0, dp(12), 0);
        brand.addView(brandMark, markLp);

        LinearLayout brandText = column();
        TextView eyebrow = monoLabel("SCANRECALL · BY ARIVEMB");
        brandText.addView(eyebrow);
        TextView title = heading("ScanRecall");
        title.setTextSize(30);
        brandText.addView(title);
        brand.addView(brandText);
        root.addView(brand);

        // Hero card
        LinearLayout hero = column();
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(roundedBackground(NAVY, CIRCUIT, 20));

        TextView heroKicker = monoLabel("PRIVATE CONTACT CAPTURE");
        hero.addView(heroKicker);

        TextView heroTitle = heading("Scan. Remember. Follow up.");
        heroTitle.setTextSize(27);
        heroTitle.setPadding(0, dp(8), 0, dp(8));
        hero.addView(heroTitle);

        TextView intro = text("Scan a business card, event badge or contact QR. ScanRecall combines visible text, QR data and meeting context into one editable contact.");
        intro.setPadding(0, 0, 0, dp(14));
        hero.addView(intro);

        LinearLayout proof = new LinearLayout(this);
        proof.setOrientation(LinearLayout.HORIZONTAL);
        proof.addView(chip("◆ OFFLINE OCR"));
        proof.addView(chip("◆ SMART QR"));
        proof.addView(chip("◆ NO CLOUD"));
        hero.addView(proof);

        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        heroLp.setMargins(0, 0, 0, dp(14));
        root.addView(hero, heroLp);

        root.addView(primaryButton("▣  SCAN PERSON / CARD / BADGE", v -> startCamera()));
        root.addView(secondaryButton("⌁  IMPORT IMAGE", v -> startGallery()));
        root.addView(secondaryButton("＋  ADD CONTACT MANUALLY", v -> {
            ContactRecord r = new ContactRecord();
            r.metDate = today();
            showEditor(r);
        }));
        root.addView(secondaryButton("◈  CONFERENCE MODE", v -> showConferenceHub()));

        section(root, "CONFERENCES · " + conferences.size());
        if (conferences.isEmpty()) {
            LinearLayout emptyConference = column();
            emptyConference.setPadding(dp(16), dp(16), dp(16), dp(16));
            emptyConference.setBackground(roundedBackground(PANEL, CIRCUIT, 16));
            emptyConference.addView(text("Create a conference once. Cards scanned inside it are automatically grouped and prefilled with that event context."));
            root.addView(emptyConference);
        } else {
            for (ConferenceRecord conference : conferences) {
                root.addView(conferenceCard(conference));
            }
        }
        root.addView(secondaryButton("＋  CREATE CONFERENCE", v -> showConferenceEditor(new ConferenceRecord())));

        section(root, "GENERAL CONTACTS · " + generalContactCount());

        searchBox = edit("⌕  Search name, company, event, tag…", "", false);
        root.addView(searchBox);
        searchBox.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                renderContactList(s.toString());
            }
        });

        contactListContainer = column();
        contactListContainer.setPadding(0, dp(8), 0, 0);
        root.addView(contactListContainer);
        renderContactList("");

        TextView footer = monoLabel("SCANRECALL · BY ARIVEMB");
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(28), 0, 0);
        root.addView(footer);

        setContentView(scroll);
    }


    private void renderContactList(String query) {
        if (contactListContainer == null) return;
        contactListContainer.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        int shown = 0;
        for (ContactRecord r : contacts) {
            if (r.conferenceId != null && !r.conferenceId.isEmpty()) continue;
            String haystack = String.join(" ",
                    safe(r.name), safe(r.company), safe(r.title), safe(r.email),
                    safe(r.event), safe(r.tags), safe(r.discussion),
                    safe(r.opportunity), safe(r.nextAction)).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !haystack.contains(q)) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackground(rippleBackground(PANEL, CIRCUIT, 16, 0x33D83FB5));
            card.setClickable(true);
            card.setFocusable(true);
            card.setElevation(dp(2));

            View accent = new View(this);
            accent.setBackground(roundedBackground(MAGENTA, MAGENTA, 3));
            LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT);
            accentLp.setMargins(0, dp(10), dp(12), dp(10));
            card.addView(accent, accentLp);

            LinearLayout content = column();
            content.setPadding(0, dp(13), dp(14), dp(13));

            TextView name = heading(r.name.isEmpty() ? "Unnamed contact" : r.name);
            name.setTextSize(18);
            content.addView(name);

            String role = joinNonBlank(" · ", r.title, r.company);
            if (!role.isEmpty()) {
                TextView roleView = smallText(role);
                roleView.setTextColor(GREY);
                roleView.setPadding(0, dp(2), 0, dp(6));
                content.addView(roleView);
            }

            if (!r.event.isEmpty()) {
                TextView event = monoLabel("◆ " + r.event);
                event.setTextSize(11);
                content.addView(event);
            }
            if (!r.nextAction.isEmpty()) {
                TextView next = smallText("→ " + r.nextAction);
                next.setTextColor(WHITE);
                next.setPadding(0, dp(7), 0, 0);
                content.addView(next);
            }
            if (!r.followUp.isEmpty()) {
                TextView follow = smallText("⌁ Follow-up  " + r.followUp);
                follow.setTextColor(CYAN);
                follow.setPadding(0, dp(3), 0, 0);
                content.addView(follow);
            }

            card.addView(content, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            card.setOnClickListener(v -> showEditor(r));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(10));
            contactListContainer.addView(card, lp);
            shown++;
        }

        if (shown == 0) {
            LinearLayout emptyCard = column();
            emptyCard.setPadding(dp(18), dp(20), dp(18), dp(20));
            emptyCard.setBackground(roundedBackground(PANEL, CIRCUIT, 16));
            TextView empty = text(contacts.isEmpty()
                    ? "No contacts yet. Scan your first business card."
                    : "No matching contacts.");
            empty.setGravity(Gravity.CENTER);
            emptyCard.addView(empty);
            contactListContainer.addView(emptyCard);
        }
    }


    private void showConferenceHub() {
        editingRecord = null;
        currentConference = null;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SILICON);

        LinearLayout root = column();
        root.setPadding(dp(18), dp(16), dp(18), dp(36));
        root.setBackgroundColor(SILICON);
        scroll.addView(root);

        Button back = ghostButton("←  BACK", v -> showHome());
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(112), dp(46));
        backLp.setMargins(0, 0, 0, dp(12));
        back.setLayoutParams(backLp);
        root.addView(back);

        root.addView(monoLabel("SCANRECALL · CONFERENCE MODE"));
        TextView title = heading("Conferences");
        title.setTextSize(28);
        title.setPadding(0, dp(5), 0, dp(3));
        root.addView(title);

        TextView helper = smallText("Create Embedded World, SMM, IAA or any event once. Contacts scanned inside it remain grouped there and inherit its event context automatically.");
        helper.setPadding(0, 0, 0, dp(12));
        root.addView(helper);

        root.addView(primaryButton("＋  CREATE CONFERENCE", v -> showConferenceEditor(new ConferenceRecord())));

        section(root, "YOUR CONFERENCES · " + conferences.size());
        if (conferences.isEmpty()) {
            LinearLayout empty = column();
            empty.setPadding(dp(18), dp(20), dp(18), dp(20));
            empty.setBackground(roundedBackground(PANEL, CIRCUIT, 16));
            TextView emptyText = text("No conferences yet.");
            emptyText.setGravity(Gravity.CENTER);
            empty.addView(emptyText);
            root.addView(empty);
        } else {
            for (ConferenceRecord conference : conferences) root.addView(conferenceCard(conference));
        }

        setContentView(scroll);
    }

    private View conferenceCard(ConferenceRecord conference) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setClickable(true);
        card.setFocusable(true);
        card.setElevation(dp(2));
        card.setBackground(rippleBackground(PANEL, CIRCUIT, 16, 0x33D83FB5));

        View accent = new View(this);
        accent.setBackground(roundedBackground(MAGENTA, MAGENTA, 3));
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT);
        accentLp.setMargins(0, dp(10), dp(12), dp(10));
        card.addView(accent, accentLp);

        LinearLayout body = column();
        body.setPadding(0, dp(13), dp(14), dp(13));
        body.addView(monoLabel("◈ CONFERENCE"));

        TextView name = heading(conference.name.isEmpty() ? "Unnamed conference" : conference.name);
        name.setTextSize(18);
        name.setPadding(0, dp(4), 0, dp(2));
        body.addView(name);

        String meta = joinNonBlank(" · ", conference.location,
                joinDateRange(conference.startDate, conference.endDate));
        if (!meta.isEmpty()) body.addView(smallText(meta));

        TextView count = smallText(conferenceContactCount(conference.id) + " contacts");
        count.setTextColor(CYAN);
        count.setPadding(0, dp(6), 0, 0);
        body.addView(count);

        card.addView(body, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = heading("→");
        arrow.setTextColor(MAGENTA_LIGHT);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.MATCH_PARENT));

        card.setOnClickListener(v -> showConference(conference));

        LinearLayout wrapper = column();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        wrapper.setLayoutParams(lp);
        wrapper.addView(card);
        return wrapper;
    }

    private void showConference(ConferenceRecord conference) {
        currentConference = conference;
        editingRecord = null;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SILICON);

        LinearLayout root = column();
        root.setPadding(dp(18), dp(16), dp(18), dp(36));
        root.setBackgroundColor(SILICON);
        scroll.addView(root);

        Button back = ghostButton("←  CONFERENCES", v -> showConferenceHub());
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(162), dp(46));
        backLp.setMargins(0, 0, 0, dp(12));
        back.setLayoutParams(backLp);
        root.addView(back);

        root.addView(monoLabel("◈ CONFERENCE MODE ACTIVE"));

        TextView title = heading(conference.name);
        title.setTextSize(28);
        title.setPadding(0, dp(5), 0, dp(3));
        root.addView(title);

        String meta = joinNonBlank(" · ", conference.location,
                joinDateRange(conference.startDate, conference.endDate));
        if (!meta.isEmpty()) {
            TextView metaView = smallText(meta);
            metaView.setPadding(0, 0, 0, dp(10));
            root.addView(metaView);
        }

        LinearLayout defaults = column();
        defaults.setPadding(dp(14), dp(12), dp(14), dp(12));
        defaults.setBackground(roundedBackground(NAVY, MAGENTA_DEEP, 14));
        defaults.addView(monoLabel("AUTO-FILLED FOR EVERY NEW CONTACT"));
        defaults.addView(smallText("Event / where we met: " + conference.name));
        if (!conference.location.isEmpty()) defaults.addView(smallText("Location: " + conference.location));
        defaults.addView(smallText("Date met: current scan date"));
        if (!conference.defaultHowMet.isEmpty()) defaults.addView(smallText("How we met: " + conference.defaultHowMet));
        if (!conference.defaultTags.isEmpty()) defaults.addView(smallText("Tags: " + conference.defaultTags));
        LinearLayout.LayoutParams defaultLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        defaultLp.setMargins(0, dp(6), 0, dp(12));
        root.addView(defaults, defaultLp);

        root.addView(primaryButton("▣  SCAN NEXT PERSON", v -> startCamera()));
        root.addView(secondaryButton("⌁  IMPORT IMAGE", v -> startGallery()));
        root.addView(secondaryButton("＋  ADD CONTACT MANUALLY", v -> {
            ContactRecord r = new ContactRecord();
            applyConferenceDefaults(r);
            showEditor(r);
        }));
        root.addView(ghostButton("✎  EDIT CONFERENCE", v -> showConferenceEditor(conference)));

        section(root, "CONTACTS · " + conferenceContactCount(conference.id));
        int shown = 0;
        for (ContactRecord r : contacts) {
            if (!conference.id.equals(r.conferenceId)) continue;
            addConferenceContactCard(root, r);
            shown++;
        }

        if (shown == 0) {
            LinearLayout empty = column();
            empty.setPadding(dp(18), dp(20), dp(18), dp(20));
            empty.setBackground(roundedBackground(PANEL, CIRCUIT, 16));
            TextView emptyText = text("No contacts in this conference yet. Scan the first card.");
            emptyText.setGravity(Gravity.CENTER);
            empty.addView(emptyText);
            root.addView(empty);
        }

        setContentView(scroll);
    }

    private void addConferenceContactCard(LinearLayout root, ContactRecord r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackground(rippleBackground(PANEL, CIRCUIT, 16, 0x33D83FB5));
        card.setClickable(true);
        card.setFocusable(true);

        View accent = new View(this);
        accent.setBackground(roundedBackground(MAGENTA, MAGENTA, 3));
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT);
        accentLp.setMargins(0, dp(10), dp(12), dp(10));
        card.addView(accent, accentLp);

        LinearLayout body = column();
        body.setPadding(0, dp(13), dp(14), dp(13));

        TextView name = heading(r.name.isEmpty() ? "Unnamed contact" : r.name);
        name.setTextSize(18);
        body.addView(name);

        String role = joinNonBlank(" · ", r.title, r.company);
        if (!role.isEmpty()) body.addView(smallText(role));

        if (!r.nextAction.isEmpty()) {
            TextView next = smallText("→ " + r.nextAction);
            next.setTextColor(WHITE);
            next.setPadding(0, dp(6), 0, 0);
            body.addView(next);
        }

        if (!r.followUp.isEmpty()) {
            TextView follow = smallText("⌁ Follow-up  " + r.followUp);
            follow.setTextColor(CYAN);
            follow.setPadding(0, dp(3), 0, 0);
            body.addView(follow);
        }

        card.addView(body, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.setOnClickListener(v -> showEditor(r));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        root.addView(card, lp);
    }

    private void showConferenceEditor(ConferenceRecord conference) {
        conferenceFields.clear();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SILICON);

        LinearLayout root = column();
        root.setPadding(dp(18), dp(16), dp(18), dp(36));
        root.setBackgroundColor(SILICON);
        scroll.addView(root);

        Button back = ghostButton("←  BACK", v -> {
            ConferenceRecord existing = findConference(conference.id);
            if (existing != null) showConference(existing);
            else showConferenceHub();
        });
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(112), dp(46));
        backLp.setMargins(0, 0, 0, dp(12));
        back.setLayoutParams(backLp);
        root.addView(back);

        root.addView(monoLabel("SCANRECALL · CONFERENCE SETUP"));

        TextView title = heading(findConference(conference.id) == null ? "Create conference" : "Edit conference");
        title.setTextSize(28);
        title.setPadding(0, dp(5), 0, dp(8));
        root.addView(title);

        addConferenceField(root, "name", "Conference name · e.g. Embedded World 2027", conference.name, false);
        addConferenceField(root, "location", "Location · e.g. Nuremberg, Germany", conference.location, false);
        addConferenceField(root, "startDate", "Start date · YYYY-MM-DD", conference.startDate, false);
        addConferenceField(root, "endDate", "End date · YYYY-MM-DD", conference.endDate, false);
        addConferenceField(root, "defaultHowMet", "Default how we met", conference.defaultHowMet, false);
        addConferenceField(root, "defaultTags", "Default tags · comma separated", conference.defaultTags, false);
        addConferenceField(root, "notes", "Conference notes", conference.notes, true);

        LinearLayout info = column();
        info.setPadding(dp(14), dp(12), dp(14), dp(12));
        info.setBackground(roundedBackground(NAVY, CIRCUIT, 14));
        info.addView(monoLabel("PREFILLING"));
        TextView infoText = smallText("Cards scanned inside this conference automatically get the conference name in Event / where we met. Location, date, how-you-met and tags are also prefilled and remain editable.");
        infoText.setPadding(0, dp(5), 0, 0);
        info.addView(infoText);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        infoLp.setMargins(0, dp(8), 0, dp(10));
        root.addView(info, infoLp);

        root.addView(primaryButton("✓  SAVE CONFERENCE", v -> {
            conference.name = conferenceField("name");
            conference.location = conferenceField("location");
            conference.startDate = conferenceField("startDate");
            conference.endDate = conferenceField("endDate");
            conference.defaultHowMet = conferenceField("defaultHowMet");
            conference.defaultTags = conferenceField("defaultTags");
            conference.notes = conferenceField("notes");

            if (conference.name.isEmpty()) {
                toast("Conference name is required");
                return;
            }

            ConferenceStore.upsert(this, conferences, conference);
            toast("Conference saved");
            showConference(conference);
        }));

        setContentView(scroll);
    }

    private void addConferenceField(LinearLayout root, String key, String hint, String value, boolean multiline) {
        EditText field = edit(hint, value, multiline);
        conferenceFields.put(key, field);
        root.addView(field);
    }

    private String conferenceField(String key) {
        EditText e = conferenceFields.get(key);
        return e == null ? "" : e.getText().toString().trim();
    }

    private void applyConferenceDefaults(ContactRecord record) {
        if (currentConference == null) return;
        record.conferenceId = currentConference.id;
        record.event = currentConference.name;
        record.location = currentConference.location;
        record.metDate = today();
        record.howMet = currentConference.defaultHowMet;
        record.tags = currentConference.defaultTags;
    }

    private ConferenceRecord findConference(String conferenceId) {
        if (conferenceId == null || conferenceId.isEmpty()) return null;
        for (ConferenceRecord conference : conferences) {
            if (conference.id.equals(conferenceId)) return conference;
        }
        return null;
    }

    private int conferenceContactCount(String conferenceId) {
        int count = 0;
        for (ContactRecord r : contacts) {
            if (conferenceId != null && conferenceId.equals(r.conferenceId)) count++;
        }
        return count;
    }

    private int generalContactCount() {
        int count = 0;
        for (ContactRecord r : contacts) {
            if (r.conferenceId == null || r.conferenceId.isEmpty()) count++;
        }
        return count;
    }

    private String joinDateRange(String startDate, String endDate) {
        if (startDate == null) startDate = "";
        if (endDate == null) endDate = "";
        if (startDate.isEmpty()) return endDate;
        if (endDate.isEmpty() || startDate.equals(endDate)) return startDate;
        return startDate + " → " + endDate;
    }

    private void returnFromEditor() {
        if (editingRecord == null) {
            if (currentConference != null) showConference(currentConference);
            else showHome();
            return;
        }

        ConferenceRecord conference = findConference(editingRecord.conferenceId);
        editingRecord = null;
        if (conference != null) showConference(conference);
        else showHome();
    }

    private void showEditor(ContactRecord record) {
        editingRecord = record;
        if (editingRecord.metDate == null || editingRecord.metDate.isEmpty()) editingRecord.metDate = today();
        editorFields.clear();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SILICON);

        LinearLayout root = column();
        root.setPadding(dp(18), dp(16), dp(18), dp(36));
        root.setBackgroundColor(SILICON);
        scroll.addView(root);

        if (record.conferenceId != null && !record.conferenceId.isEmpty() && currentConference == null) {
            currentConference = findConference(record.conferenceId);
        }

        Button back = ghostButton("←  BACK", v -> returnFromEditor());
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(112), dp(46));
        backLp.setMargins(0, 0, 0, dp(12));
        back.setLayoutParams(backLp);
        root.addView(back);

        TextView kicker = monoLabel("SCANRECALL · CONTACT REVIEW");
        root.addView(kicker);

        TextView title = heading("Review contact");
        title.setTextSize(28);
        title.setPadding(0, dp(5), 0, dp(3));
        root.addView(title);

        TextView helper = smallText("OCR fields are editable. Keep only what is useful.");
        helper.setPadding(0, 0, 0, dp(8));
        root.addView(helper);

        section(root, "01 · CONTACT");
        addField(root, "name", "Name", record.name, false);
        addField(root, "company", "Company", record.company, false);
        addField(root, "title", "Position / title", record.title, false);
        addField(root, "email", "Email", record.email, false, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        addField(root, "phone", "Phone", record.phone, false, InputType.TYPE_CLASS_PHONE);
        addField(root, "mobile", "Mobile", record.mobile, false, InputType.TYPE_CLASS_PHONE);
        addField(root, "website", "Website", record.website, false, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addField(root, "address", "Address", record.address, true);

        if (record.conferenceId != null && !record.conferenceId.isEmpty()) {
            ConferenceRecord linkedConference = findConference(record.conferenceId);
            if (linkedConference != null) {
                LinearLayout conferenceContext = column();
                conferenceContext.setPadding(dp(14), dp(12), dp(14), dp(12));
                conferenceContext.setBackground(roundedBackground(NAVY, MAGENTA_DEEP, 14));
                conferenceContext.addView(monoLabel("◈ CONFERENCE CONTEXT"));

                TextView contextName = heading(linkedConference.name);
                contextName.setTextSize(18);
                contextName.setPadding(0, dp(5), 0, dp(2));
                conferenceContext.addView(contextName);

                String contextMeta = joinNonBlank(" · ", linkedConference.location,
                        joinDateRange(linkedConference.startDate, linkedConference.endDate));
                if (!contextMeta.isEmpty()) conferenceContext.addView(smallText(contextMeta));

                LinearLayout.LayoutParams contextLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                contextLp.setMargins(0, dp(16), 0, 0);
                root.addView(conferenceContext, contextLp);
            }
        }

        section(root, "02 · MEETING CONTEXT");
        addField(root, "event", "Event / where we met", record.event, false);
        addField(root, "metDate", "Date met (YYYY-MM-DD)", record.metDate, false);
        addField(root, "location", "Location", record.location, false);
        addField(root, "howMet", "How we met", record.howMet, false);
        addField(root, "relevance", "Why this contact matters", record.relevance, true);
        addField(root, "discussion", "What we discussed", record.discussion, true);
        addField(root, "opportunity", "Opportunity / use case", record.opportunity, true);

        section(root, "03 · FOLLOW-UP");
        addField(root, "nextAction", "Next action", record.nextAction, true);
        addField(root, "followUp", "Follow-up date", record.followUp, false);
        addField(root, "status", "Status · New / Follow-up / Opportunity / Customer", record.status, false);
        addField(root, "priority", "Priority · Low / Normal / High", record.priority, false);
        addField(root, "tags", "Tags · comma separated", record.tags, false);
        addField(root, "notes", "General notes", record.notes, true);

        if (record.rawText != null && !record.rawText.isEmpty()) {
            section(root, "OCR SOURCE");
            LinearLayout rawPanel = column();
            rawPanel.setPadding(dp(14), dp(12), dp(14), dp(12));
            rawPanel.setBackground(roundedBackground(NAVY, CIRCUIT, 14));
            TextView raw = smallText(record.rawText);
            raw.setTextColor(GREY);
            raw.setTypeface(Typeface.MONOSPACE);
            raw.setTextIsSelectable(true);
            rawPanel.addView(raw);
            root.addView(rawPanel);
        }

        LinearLayout privacyPanel = column();
        privacyPanel.setPadding(dp(14), dp(12), dp(14), dp(12));
        privacyPanel.setBackground(roundedBackground(0xff0D1C27, 0xff1A6270, 14));
        TextView privacyLabel = monoLabel("◆ LOCAL-FIRST");
        privacyLabel.setTextColor(CYAN);
        privacyPanel.addView(privacyLabel);
        TextView privacy = smallText("ScanRecall stores this record locally on this device. Saving to phone contacts opens Android's normal contact-save screen.");
        privacy.setTextColor(0xffC6CEDA);
        privacy.setPadding(0, dp(5), 0, 0);
        privacyPanel.addView(privacy);
        LinearLayout.LayoutParams privacyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        privacyLp.setMargins(0, dp(18), 0, dp(10));
        root.addView(privacyPanel, privacyLp);

        root.addView(primaryButton("✓  SAVE IN SCANRECALL", v -> {
            captureEditorIntoRecord();
            ContactStore.upsert(this, contacts, editingRecord);
            toast("Saved");
            returnFromEditor();
        }));

        root.addView(secondaryButton("＋  SAVE TO PHONE CONTACTS", v -> {
            captureEditorIntoRecord();
            ContactStore.upsert(this, contacts, editingRecord);
            openNativeContactInsert(editingRecord);
        }));

        root.addView(secondaryButton("⇩  EXPORT VCARD (.VCF)", v -> {
            captureEditorIntoRecord();
            ContactStore.upsert(this, contacts, editingRecord);
            startVCardExport(editingRecord);
        }));

        Button delete = dangerButton("×  DELETE RECORD", v -> deleteCurrentRecord());
        root.addView(delete);

        setContentView(scroll);
    }


    private void addField(LinearLayout root, String key, String hint, String value, boolean multiline) {
        addField(root, key, hint, value, multiline, InputType.TYPE_CLASS_TEXT);
    }

    private void addField(LinearLayout root, String key, String hint, String value, boolean multiline, int inputType) {
        EditText e = edit(hint, value, multiline);
        e.setInputType(inputType | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (multiline) {
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        }
        editorFields.put(key, e);
        root.addView(e);
    }

    private void captureEditorIntoRecord() {
        if (editingRecord == null) return;
        editingRecord.name = field("name");
        editingRecord.company = field("company");
        editingRecord.title = field("title");
        editingRecord.email = field("email");
        editingRecord.phone = field("phone");
        editingRecord.mobile = field("mobile");
        editingRecord.website = field("website");
        editingRecord.address = field("address");

        editingRecord.event = field("event");
        editingRecord.metDate = field("metDate");
        editingRecord.location = field("location");
        editingRecord.howMet = field("howMet");
        editingRecord.relevance = field("relevance");
        editingRecord.discussion = field("discussion");
        editingRecord.opportunity = field("opportunity");

        editingRecord.nextAction = field("nextAction");
        editingRecord.followUp = field("followUp");
        editingRecord.status = field("status");
        editingRecord.priority = field("priority");
        editingRecord.tags = field("tags");
        editingRecord.notes = field("notes");
    }

    private String field(String key) {
        EditText e = editorFields.get(key);
        return e == null ? "" : e.getText().toString().trim();
    }

    private void deleteCurrentRecord() {
        if (editingRecord == null) return;
        ContactRecord target = editingRecord;
        String conferenceId = target.conferenceId;
        contacts.removeIf(r -> r.id == target.id);
        ContactStore.save(this, contacts);
        if (target.imagePath != null && !target.imagePath.isEmpty()) {
            try { new File(target.imagePath).delete(); } catch (Exception ignored) {}
        }
        toast("Deleted");
        editingRecord = null;
        ConferenceRecord conference = findConference(conferenceId);
        if (conference != null) showConference(conference);
        else showHome();
    }

    private void startCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "cardkeep_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ScanRecallTemp");

            pendingCameraUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (pendingCameraUri == null) throw new IllegalStateException("Could not create image destination");

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) == null) {
                safeDeleteUri(pendingCameraUri);
                pendingCameraUri = null;
                toast("No camera app is available");
                return;
            }
            startActivityForResult(intent, REQ_CAMERA);
        } catch (Exception e) {
            toast("Could not open camera: " + safeMessage(e));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                toast("Camera permission is required for Smart Scan. You can enable it in Android Settings > Apps > ScanRecall > Permissions.");
            }
        }
    }

    private void startGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_GALLERY);
    }

    private void startVCardExport(ContactRecord record) {
        pendingExportRecord = record;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/x-vcard");
        intent.putExtra(Intent.EXTRA_TITLE, safeFileName(record.name.isEmpty() ? "contact" : record.name) + ".vcf");
        startActivityForResult(intent, REQ_EXPORT_VCARD);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CAMERA) {
            Uri uri = pendingCameraUri;
            pendingCameraUri = null;
            if (resultCode == RESULT_OK && uri != null) {
                processCardImage(uri, true);
            } else if (uri != null) {
                safeDeleteUri(uri);
            }
            return;
        }

        if (requestCode == REQ_GALLERY && resultCode == RESULT_OK && data != null && data.getData() != null) {
            processCardImage(data.getData(), false);
            return;
        }

        if (requestCode == REQ_EXPORT_VCARD && resultCode == RESULT_OK && data != null && data.getData() != null && pendingExportRecord != null) {
            writeVCard(data.getData(), pendingExportRecord);
            pendingExportRecord = null;
        }
    }

    private void processCardImage(Uri sourceUri, boolean deleteSourceAfter) {
        ProgressDialog dialog = ProgressDialog.show(this, "ScanRecall", "Reading card, badge and QR…", true, false);
        try {
            File privateCopy = copyToPrivateStorage(sourceUri);
            InputImage image = InputImage.fromFilePath(this, sourceUri);
            BarcodeScanner barcodeScanner = BarcodeScanning.getClient();

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        List<String> qrValues = new ArrayList<>();
                        for (Barcode barcode : barcodes) {
                            String raw = barcode.getRawValue();
                            if (raw != null && !raw.trim().isEmpty()) qrValues.add(raw);
                        }
                        barcodeScanner.close();
                        runOcrAndMerge(image, qrValues, privateCopy, sourceUri, deleteSourceAfter, dialog);
                    })
                    .addOnFailureListener(error -> {
                        barcodeScanner.close();
                        runOcrAndMerge(image, new ArrayList<>(), privateCopy, sourceUri, deleteSourceAfter, dialog);
                    });
        } catch (Exception e) {
            dialog.dismiss();
            if (deleteSourceAfter) safeDeleteUri(sourceUri);
            toast("Could not process image: " + safeMessage(e));
        }
    }

    private void runOcrAndMerge(InputImage image,
                                List<String> qrValues,
                                File privateCopy,
                                Uri sourceUri,
                                boolean deleteSourceAfter,
                                ProgressDialog dialog) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    String primaryText = result.getText();
                    recognizer.close();
                    runEnhancedOcr(primaryText, qrValues, privateCopy, sourceUri, deleteSourceAfter, dialog);
                })
                .addOnFailureListener(error -> {
                    recognizer.close();
                    runEnhancedOcr("", qrValues, privateCopy, sourceUri, deleteSourceAfter, dialog);
                });
    }

    private void runEnhancedOcr(String primaryText,
                                List<String> qrValues,
                                File privateCopy,
                                Uri sourceUri,
                                boolean deleteSourceAfter,
                                ProgressDialog dialog) {
        final Bitmap enhanced;
        try {
            enhanced = createColorRobustOcrBitmap(sourceUri);
        } catch (Exception e) {
            finishOcr(primaryText, qrValues, privateCopy, sourceUri, deleteSourceAfter, dialog);
            return;
        }

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage enhancedImage = InputImage.fromBitmap(enhanced, 0);
        recognizer.process(enhancedImage)
                .addOnSuccessListener(result -> {
                    recognizer.close();
                    String secondaryText = result.getText();
                    enhanced.recycle();
                    finishOcr(mergeOcrText(primaryText, secondaryText),
                            qrValues, privateCopy, sourceUri, deleteSourceAfter, dialog);
                })
                .addOnFailureListener(error -> {
                    recognizer.close();
                    enhanced.recycle();
                    finishOcr(primaryText, qrValues, privateCopy, sourceUri, deleteSourceAfter, dialog);
                });
    }

    private void finishOcr(String ocrText,
                           List<String> qrValues,
                           File privateCopy,
                           Uri sourceUri,
                           boolean deleteSourceAfter,
                           ProgressDialog dialog) {
        dialog.dismiss();
        if (deleteSourceAfter) safeDeleteUri(sourceUri);

        ContactRecord r = (ocrText == null || ocrText.trim().isEmpty())
                ? new ContactRecord()
                : BusinessCardParser.parse(ocrText);
        QrPayloadParser.MergeResult qr = QrPayloadParser.mergeInto(r, qrValues);
        r.imagePath = privateCopy.getAbsolutePath();
        applyConferenceDefaults(r);
        if (r.metDate == null || r.metDate.isEmpty()) r.metDate = today();

        if (qr.codeCount > 0) {
            if (ocrText == null || ocrText.trim().isEmpty()) {
                toast(qr.kind + " detected. Visible text could not be read; review the QR-derived details.");
            } else {
                toast(qr.kind + " detected · visible badge/card text was also read.");
            }
        } else if (ocrText == null || ocrText.trim().isEmpty()) {
            toast("ScanRecall could not read this image. You can enter the details manually.");
        }
        showEditor(r);
    }

    private Bitmap createColorRobustOcrBitmap(Uri sourceUri) throws Exception {
        ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), sourceUri);
        Bitmap bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            int width = info.getSize().getWidth();
            int height = info.getSize().getHeight();
            int max = Math.max(width, height);
            final int targetMax = 2400;
            if (max > targetMax) {
                float scale = (float) targetMax / (float) max;
                decoder.setTargetSize(
                        Math.max(1, Math.round(width * scale)),
                        Math.max(1, Math.round(height * scale)));
            }
        });

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int alpha = Color.alpha(p);
            int minChannel = Math.min(Color.red(p), Math.min(Color.green(p), Color.blue(p)));

            // Using the darkest RGB channel makes red, blue and other coloured text
            // substantially darker than white/light card stock. Contrast stretching
            // then improves small punctuation such as hyphens in e-mail addresses.
            int value = ((minChannel - 30) * 255) / 205;
            value = Math.max(0, Math.min(255, value));
            pixels[i] = Color.argb(alpha, value, value, value);
        }

        Bitmap enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        enhanced.setPixels(pixels, 0, width, 0, 0, width, height);
        bitmap.recycle();
        return enhanced;
    }

    private String mergeOcrText(String primary, String secondary) {
        // Keep the union of text found by both OCR passes. The original-image
        // pass is best for normal dark text; the colour-robust pass recovers
        // coloured/light headings and small punctuation. Exact duplicate lines
        // are removed, but differing OCR readings are intentionally retained so
        // the raw contact note remains a faithful fallback record.
        List<String> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        appendOcrLines(primary, merged, seen);
        appendOcrLines(secondary, merged, seen);
        return String.join("\n", merged).trim();
    }

    private void appendOcrLines(String text, List<String> merged, Set<String> seen) {
        if (text == null || text.trim().isEmpty()) return;
        for (String line : text.split("\\r?\\n")) {
            String cleaned = line.trim().replaceAll("\\s{2,}", " ");
            if (cleaned.isEmpty()) continue;
            String key = cleaned.toLowerCase(Locale.ROOT);
            if (seen.add(key)) merged.add(cleaned);
        }
    }

    private File copyToPrivateStorage(Uri sourceUri) throws Exception {
        File dir = new File(getFilesDir(), "cards");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Could not create card storage");

        File target = new File(dir, "card_" + System.currentTimeMillis() + ".jpg");
        try (InputStream in = getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IllegalStateException("Could not open image");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        }
        return target;
    }

    private void safeDeleteUri(Uri uri) {
        try { getContentResolver().delete(uri, null, null); } catch (Exception ignored) {}
    }

    private void openNativeContactInsert(ContactRecord r) {
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.NAME, r.name);
        intent.putExtra(ContactsContract.Intents.Insert.COMPANY, r.company);
        intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, r.title);
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, r.email);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, !r.mobile.isEmpty() ? r.mobile : r.phone);
        intent.putExtra(ContactsContract.Intents.Insert.POSTAL, r.address);
        intent.putExtra(ContactsContract.Intents.Insert.NOTES, portableNotes(r));

        ArrayList<ContentValues> data = new ArrayList<>();
        if (!r.website.isEmpty()) {
            ContentValues website = new ContentValues();
            website.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
            website.put(ContactsContract.CommonDataKinds.Website.URL, r.website);
            website.put(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_WORK);
            data.add(website);
        }
        if (!r.phone.isEmpty() && !r.mobile.isEmpty()) {
            ContentValues workPhone = new ContentValues();
            workPhone.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            workPhone.put(ContactsContract.CommonDataKinds.Phone.NUMBER, r.phone);
            workPhone.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
            data.add(workPhone);
        }
        if (!data.isEmpty()) intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);

        try {
            startActivity(intent);
        } catch (Exception e) {
            toast("No contacts app is available");
        }
    }

    private void writeVCard(Uri destination, ContactRecord r) {
        try (OutputStream out = getContentResolver().openOutputStream(destination)) {
            if (out == null) throw new IllegalStateException("Could not create vCard");
            out.write(buildVCard(r).getBytes(StandardCharsets.UTF_8));
            toast("vCard exported");
        } catch (Exception e) {
            toast("Could not export vCard: " + safeMessage(e));
        }
    }

    private String buildVCard(ContactRecord r) {
        StringBuilder b = new StringBuilder();
        b.append("BEGIN:VCARD\r\n");
        b.append("VERSION:3.0\r\n");
        b.append("FN:").append(vcardEscape(r.name)).append("\r\n");
        if (!r.company.isEmpty()) b.append("ORG:").append(vcardEscape(r.company)).append("\r\n");
        if (!r.title.isEmpty()) b.append("TITLE:").append(vcardEscape(r.title)).append("\r\n");
        if (!r.mobile.isEmpty()) b.append("TEL;TYPE=CELL:").append(vcardEscape(r.mobile)).append("\r\n");
        if (!r.phone.isEmpty()) b.append("TEL;TYPE=WORK:").append(vcardEscape(r.phone)).append("\r\n");
        if (!r.email.isEmpty()) b.append("EMAIL;TYPE=INTERNET:").append(vcardEscape(r.email)).append("\r\n");
        if (!r.website.isEmpty()) b.append("URL:").append(vcardEscape(r.website)).append("\r\n");
        if (!r.address.isEmpty()) b.append("ADR;TYPE=WORK:;;").append(vcardEscape(r.address)).append(";;;;\r\n");

        String note = portableNotes(r);
        if (!note.isEmpty()) b.append("NOTE:").append(vcardEscape(note)).append("\r\n");
        b.append("END:VCARD\r\n");
        return b.toString();
    }

    private String portableNotes(ContactRecord r) {
        StringBuilder b = new StringBuilder();
        addNote(b, "Met at", r.event);
        addNote(b, "Date", r.metDate);
        addNote(b, "Location", r.location);
        addNote(b, "How met", r.howMet);
        addNote(b, "Why relevant", r.relevance);
        addNote(b, "Discussed", r.discussion);
        addNote(b, "Opportunity", r.opportunity);
        addNote(b, "Next action", r.nextAction);
        addNote(b, "Follow-up", r.followUp);
        addNote(b, "Status", r.status);
        addNote(b, "Priority", r.priority);
        addNote(b, "Tags", r.tags);
        addNote(b, "Notes", r.notes);

        String rawOcr = safe(r.rawText).trim();
        if (!rawOcr.isEmpty()) {
            if (b.length() > 0) b.append("\n\n");
            b.append("--- SCANRECALL OCR RAW TEXT ---\n");
            b.append(rawOcr);
        }
        return b.toString().trim();
    }

    private void addNote(StringBuilder b, String label, String value) {
        if (value != null && !value.isEmpty()) {
            if (b.length() > 0) b.append("\n");
            b.append(label).append(": ").append(value);
        }
    }

    private String vcardEscape(String s) {
        return safe(s)
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return l;
    }

    private TextView heading(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(WHITE);
        t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        t.setTextSize(20);
        t.setLineSpacing(0, 1.06f);
        return t;
    }

    private TextView text(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(16);
        t.setTextColor(0xffC6CEDA);
        t.setLineSpacing(dp(2), 1.12f);
        return t;
    }

    private TextView smallText(String value) {
        TextView t = text(value);
        t.setTextSize(14);
        t.setTextColor(GREY);
        return t;
    }

    private TextView monoLabel(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(MAGENTA_LIGHT);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        t.setTextSize(11);
        t.setLetterSpacing(0.10f);
        return t;
    }

    private TextView chip(String value) {
        TextView t = monoLabel(value);
        t.setTextSize(9);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(6), dp(8), dp(6));
        t.setBackground(roundedBackground(PANEL_2, MAGENTA_DEEP, 999));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(0, 0, dp(6), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private void section(LinearLayout root, String value) {
        TextView t = monoLabel(value);
        t.setTextSize(12);
        t.setPadding(0, dp(24), 0, dp(8));
        root.addView(t);
    }

    private EditText edit(String hint, String value, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0xff748196);
        e.setTextColor(WHITE);
        e.setText(value == null ? "" : value);
        e.setTextSize(16);
        e.setSingleLine(!multiline);
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setBackground(roundedBackground(PANEL_2, CIRCUIT, 13));
        if (multiline) {
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setMinHeight(dp(52));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(9));
        e.setLayoutParams(lp);
        return e;
    }

    private Button primaryButton(String label, View.OnClickListener listener) {
        Button b = baseButton(label, listener);
        b.setTextColor(Color.WHITE);
        b.setBackground(rippleBackground(MAGENTA, MAGENTA, 14, 0x44FFFFFF));
        b.setElevation(dp(4));
        return b;
    }

    private Button secondaryButton(String label, View.OnClickListener listener) {
        Button b = baseButton(label, listener);
        b.setTextColor(WHITE);
        b.setBackground(rippleBackground(PANEL, CIRCUIT, 14, 0x33D83FB5));
        return b;
    }

    private Button ghostButton(String label, View.OnClickListener listener) {
        Button b = baseButton(label, listener);
        b.setTextColor(MAGENTA_LIGHT);
        b.setTextSize(12);
        b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        b.setBackground(rippleBackground(SILICON, CIRCUIT, 12, 0x33D83FB5));
        return b;
    }

    private Button dangerButton(String label, View.OnClickListener listener) {
        Button b = baseButton(label, listener);
        b.setTextColor(0xffFF9AA8);
        b.setBackground(rippleBackground(0xff201218, 0xff682B3A, 14, 0x33FF667D));
        return b;
    }

    private Button baseButton(String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        b.setLetterSpacing(0.04f);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(54));
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54));
        lp.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(lp);
        return b;
    }

    private GradientDrawable roundedBackground(int fill, int stroke, float radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(Math.round(radiusDp)));
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private RippleDrawable rippleBackground(int fill, int stroke, float radiusDp, int rippleColor) {
        GradientDrawable content = roundedBackground(fill, stroke, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private String safeFileName(String s) {
        String cleaned = safe(s).trim().replaceAll("[^A-Za-z0-9._-]+", "_");
        return cleaned.isEmpty() ? "contact" : cleaned;
    }

    private String joinNonBlank(String delimiter, String... values) {
        StringBuilder b = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (b.length() > 0) b.append(delimiter);
            b.append(value.trim());
        }
        return b.toString();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
