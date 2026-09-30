package com.madhankurukondar.cardkeep;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 100;
    private static final int REQ_GALLERY = 101;
    private static final int REQ_EXPORT_VCARD = 102;

    private final List<ContactRecord> contacts = new ArrayList<>();
    private final Map<String, EditText> editorFields = new LinkedHashMap<>();

    private Uri pendingCameraUri;
    private ContactRecord editingRecord;
    private ContactRecord pendingExportRecord;
    private LinearLayout contactListContainer;
    private EditText searchBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contacts.addAll(ContactStore.load(this));
        showHome();
    }

    @Override
    public void onBackPressed() {
        if (editingRecord != null) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    private void showHome() {
        editingRecord = null;
        editorFields.clear();

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column();
        root.setPadding(dp(18), dp(18), dp(18), dp(32));
        scroll.addView(root);

        TextView title = heading("CardKeep");
        title.setTextSize(28);
        root.addView(title);

        TextView intro = text("Private business-card capture. Scan a card, keep why the contact matters and what you discussed, then save it to your phone contacts. No account, no ads, no CardKeep cloud.");
        intro.setPadding(0, dp(6), 0, dp(16));
        root.addView(intro);

        root.addView(primaryButton("Scan business card", v -> startCamera()));
        root.addView(secondaryButton("Import card image", v -> startGallery()));
        root.addView(secondaryButton("Add contact manually", v -> {
            ContactRecord r = new ContactRecord();
            r.metDate = today();
            showEditor(r);
        }));

        section(root, "Contacts");

        searchBox = edit("Search name, company, event, tag…", "", false);
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

        setContentView(scroll);
    }

    private void renderContactList(String query) {
        if (contactListContainer == null) return;
        contactListContainer.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        int shown = 0;
        for (ContactRecord r : contacts) {
            String haystack = String.join(" ",
                    safe(r.name), safe(r.company), safe(r.title), safe(r.email),
                    safe(r.event), safe(r.tags), safe(r.discussion),
                    safe(r.opportunity), safe(r.nextAction)).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !haystack.contains(q)) continue;

            LinearLayout card = column();
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackgroundColor(0xfff3f5f7);

            TextView name = heading(r.name.isEmpty() ? "Unnamed contact" : r.name);
            name.setTextSize(18);
            card.addView(name);

            String role = joinNonBlank(" · ", r.title, r.company);
            if (!role.isEmpty()) card.addView(text(role));
            if (!r.event.isEmpty()) card.addView(smallText("Met at: " + r.event));
            if (!r.nextAction.isEmpty()) card.addView(smallText("Next: " + r.nextAction));
            if (!r.followUp.isEmpty()) card.addView(smallText("Follow-up: " + r.followUp));

            card.setClickable(true);
            card.setOnClickListener(v -> showEditor(r));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(10));
            contactListContainer.addView(card, lp);
            shown++;
        }

        if (shown == 0) {
            TextView empty = text(contacts.isEmpty()
                    ? "No contacts yet. Scan your first business card."
                    : "No matching contacts.");
            empty.setPadding(0, dp(14), 0, 0);
            contactListContainer.addView(empty);
        }
    }

    private void showEditor(ContactRecord record) {
        editingRecord = record;
        if (editingRecord.metDate == null || editingRecord.metDate.isEmpty()) editingRecord.metDate = today();
        editorFields.clear();

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column();
        root.setPadding(dp(18), dp(14), dp(18), dp(32));
        scroll.addView(root);

        Button back = secondaryButton("← Back", v -> showHome());
        root.addView(back);

        TextView title = heading("Review contact");
        title.setTextSize(26);
        title.setPadding(0, dp(8), 0, dp(8));
        root.addView(title);

        section(root, "Contact");
        addField(root, "name", "Name", record.name, false);
        addField(root, "company", "Company", record.company, false);
        addField(root, "title", "Position / title", record.title, false);
        addField(root, "email", "Email", record.email, false, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        addField(root, "phone", "Phone", record.phone, false, InputType.TYPE_CLASS_PHONE);
        addField(root, "mobile", "Mobile", record.mobile, false, InputType.TYPE_CLASS_PHONE);
        addField(root, "website", "Website", record.website, false, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addField(root, "address", "Address", record.address, true);

        section(root, "Meeting context");
        addField(root, "event", "Event / where we met", record.event, false);
        addField(root, "metDate", "Date met (YYYY-MM-DD)", record.metDate, false);
        addField(root, "location", "Location", record.location, false);
        addField(root, "howMet", "How we met", record.howMet, false);
        addField(root, "relevance", "Why this contact matters", record.relevance, true);
        addField(root, "discussion", "What we discussed", record.discussion, true);
        addField(root, "opportunity", "Opportunity / use case", record.opportunity, true);

        section(root, "Follow-up");
        addField(root, "nextAction", "Next action", record.nextAction, true);
        addField(root, "followUp", "Follow-up date", record.followUp, false);
        addField(root, "status", "Status (New / Follow-up / Opportunity / Customer)", record.status, false);
        addField(root, "priority", "Priority (Low / Normal / High)", record.priority, false);
        addField(root, "tags", "Tags (comma separated)", record.tags, false);
        addField(root, "notes", "General notes", record.notes, true);

        if (record.rawText != null && !record.rawText.isEmpty()) {
            section(root, "Original OCR text");
            TextView raw = smallText(record.rawText);
            raw.setTextIsSelectable(true);
            root.addView(raw);
        }

        TextView privacy = smallText("CardKeep stores this record locally on this device. Saving to phone contacts opens Android's normal contact-save screen.");
        privacy.setPadding(0, dp(16), 0, dp(8));
        root.addView(privacy);

        root.addView(primaryButton("Save in CardKeep", v -> {
            captureEditorIntoRecord();
            ContactStore.upsert(this, contacts, editingRecord);
            toast("Saved");
            showHome();
        }));

        root.addView(secondaryButton("Save to phone contacts", v -> {
            captureEditorIntoRecord();
            ContactStore.upsert(this, contacts, editingRecord);
            openNativeContactInsert(editingRecord);
        }));

        root.addView(secondaryButton("Export vCard (.vcf)", v -> {
            captureEditorIntoRecord();
            ContactStore.upsert(this, contacts, editingRecord);
            startVCardExport(editingRecord);
        }));

        Button delete = secondaryButton("Delete CardKeep record", v -> deleteCurrentRecord());
        delete.setTextColor(0xffa00000);
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
        contacts.removeIf(r -> r.id == target.id);
        ContactStore.save(this, contacts);
        if (target.imagePath != null && !target.imagePath.isEmpty()) {
            try { new File(target.imagePath).delete(); } catch (Exception ignored) {}
        }
        toast("Deleted");
        showHome();
    }

    private void startCamera() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "cardkeep_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CardKeepTemp");

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
        ProgressDialog dialog = ProgressDialog.show(this, "CardKeep", "Reading business card…", true, false);
        try {
            File privateCopy = copyToPrivateStorage(sourceUri);
            InputImage image = InputImage.fromFilePath(this, sourceUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        recognizer.close();
                        dialog.dismiss();
                        if (deleteSourceAfter) safeDeleteUri(sourceUri);

                        ContactRecord r = BusinessCardParser.parse(result.getText());
                        r.imagePath = privateCopy.getAbsolutePath();
                        r.metDate = today();
                        showEditor(r);
                    })
                    .addOnFailureListener(error -> {
                        recognizer.close();
                        dialog.dismiss();
                        if (deleteSourceAfter) safeDeleteUri(sourceUri);

                        ContactRecord r = new ContactRecord();
                        r.imagePath = privateCopy.getAbsolutePath();
                        r.metDate = today();
                        toast("OCR could not read this card. You can enter the details manually.");
                        showEditor(r);
                    });
        } catch (Exception e) {
            dialog.dismiss();
            if (deleteSourceAfter) safeDeleteUri(sourceUri);
            toast("Could not process image: " + safeMessage(e));
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
        t.setTextColor(0xff111111);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextSize(20);
        return t;
    }

    private TextView text(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(16);
        t.setTextColor(0xff222222);
        return t;
    }

    private TextView smallText(String value) {
        TextView t = text(value);
        t.setTextSize(14);
        t.setTextColor(0xff555555);
        return t;
    }

    private void section(LinearLayout root, String value) {
        TextView t = heading(value);
        t.setPadding(0, dp(20), 0, dp(6));
        root.addView(t);
    }

    private EditText edit(String hint, String value, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setTextSize(16);
        e.setSingleLine(!multiline);
        if (multiline) {
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        e.setLayoutParams(lp);
        return e;
    }

    private Button primaryButton(String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private Button secondaryButton(String label, View.OnClickListener listener) {
        return primaryButton(label, listener);
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
