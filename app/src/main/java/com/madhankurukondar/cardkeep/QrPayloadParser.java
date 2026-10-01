package com.madhankurukondar.cardkeep;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class QrPayloadParser {
    private QrPayloadParser() {}

    public static final class MergeResult {
        public final int codeCount;
        public final String kind;
        public final boolean addedContactData;

        MergeResult(int codeCount, String kind, boolean addedContactData) {
            this.codeCount = codeCount;
            this.kind = kind;
            this.addedContactData = addedContactData;
        }
    }

    public static MergeResult mergeInto(ContactRecord record, List<String> rawValues) {
        if (record == null || rawValues == null || rawValues.isEmpty()) {
            return new MergeResult(0, "", false);
        }

        int count = 0;
        boolean contactData = false;
        boolean profileUrl = false;
        boolean opaque = false;

        List<String> identifiers = new ArrayList<>();

        for (String raw : rawValues) {
            String value = safe(raw).trim();
            if (value.isEmpty()) continue;
            count++;

            String upper = value.toUpperCase(Locale.ROOT);
            if (upper.startsWith("BEGIN:VCARD")) {
                contactData |= parseVCard(record, value);
                record.tags = addTag(record.tags, "QR");
                continue;
            }

            if (upper.startsWith("MECARD:")) {
                contactData |= parseMeCard(record, value.substring(7));
                record.tags = addTag(record.tags, "QR");
                continue;
            }

            if (upper.startsWith("MAILTO:")) {
                String email = decode(value.substring(7)).split("\\?")[0];
                if (!email.isBlank()) {
                    record.email = email;
                    contactData = true;
                }
                record.tags = addTag(record.tags, "QR");
                continue;
            }

            if (upper.startsWith("TEL:")) {
                String phone = decode(value.substring(4));
                if (!phone.isBlank()) {
                    record.mobile = phone;
                    contactData = true;
                }
                record.tags = addTag(record.tags, "QR");
                continue;
            }

            if (isUrl(value)) {
                record.website = value;
                profileUrl = true;
                record.tags = addTag(record.tags, "QR");
                if (value.toLowerCase(Locale.ROOT).contains("linkedin.com")) {
                    record.tags = addTag(record.tags, "LinkedIn");
                }
                continue;
            }

            // Event badges often encode only an opaque attendee ID or token.
            // Keep it as context; do not pretend it contains personal data.
            opaque = true;
            identifiers.add(value);
            record.tags = addTag(record.tags, "Badge QR");
        }

        if (!identifiers.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (String id : identifiers) {
                if (b.length() > 0) b.append("\n");
                b.append("Event badge QR / identifier: ").append(id);
            }
            record.notes = append(record.notes, b.toString());
        }

        String kind;
        if (contactData) kind = "Contact QR";
        else if (profileUrl) kind = "Profile / URL QR";
        else if (opaque) kind = "Event badge QR";
        else kind = count > 0 ? "QR / barcode" : "";

        return new MergeResult(count, kind, contactData);
    }

    private static boolean parseVCard(ContactRecord r, String raw) {
        boolean changed = false;
        String normalized = raw.replace("\r\n ", "").replace("\n ", "");
        String[] lines = normalized.split("\\r?\\n");

        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String key = line.substring(0, colon).toUpperCase(Locale.ROOT);
            String value = unescape(line.substring(colon + 1).trim());
            if (value.isEmpty()) continue;

            if (key.equals("FN") || key.startsWith("FN;")) {
                r.name = value;
                changed = true;
            } else if ((key.equals("N") || key.startsWith("N;")) && safe(r.name).isBlank()) {
                String[] parts = value.split(";", -1);
                StringBuilder name = new StringBuilder();
                if (parts.length > 1 && !parts[1].isBlank()) name.append(parts[1].trim());
                if (parts.length > 0 && !parts[0].isBlank()) {
                    if (name.length() > 0) name.append(' ');
                    name.append(parts[0].trim());
                }
                if (name.length() > 0) {
                    r.name = name.toString();
                    changed = true;
                }
            } else if (key.equals("ORG") || key.startsWith("ORG;")) {
                r.company = value.replace(';', ' ');
                changed = true;
            } else if (key.equals("TITLE") || key.startsWith("TITLE;")) {
                r.title = value;
                changed = true;
            } else if (key.startsWith("EMAIL")) {
                r.email = value;
                changed = true;
            } else if (key.startsWith("TEL")) {
                if (key.contains("CELL") || key.contains("MOBILE")) r.mobile = value;
                else if (safe(r.phone).isBlank()) r.phone = value;
                else if (safe(r.mobile).isBlank()) r.mobile = value;
                changed = true;
            } else if (key.startsWith("URL")) {
                r.website = value;
                changed = true;
            } else if (key.startsWith("ADR")) {
                r.address = value.replace(';', ' ').replaceAll("\\s+", " ").trim();
                changed = true;
            } else if (key.startsWith("NOTE")) {
                r.notes = append(r.notes, value);
            }
        }
        return changed;
    }

    private static boolean parseMeCard(ContactRecord r, String payload) {
        boolean changed = false;
        String[] fields = payload.split(";");
        for (String field : fields) {
            int colon = field.indexOf(':');
            if (colon <= 0) continue;
            String key = field.substring(0, colon).trim().toUpperCase(Locale.ROOT);
            String value = unescape(field.substring(colon + 1).trim());
            if (value.isEmpty()) continue;

            switch (key) {
                case "N":
                    String[] n = value.split(",", -1);
                    if (n.length > 1) r.name = (n[1] + " " + n[0]).trim();
                    else r.name = value;
                    changed = true;
                    break;
                case "ORG":
                    r.company = value;
                    changed = true;
                    break;
                case "TEL":
                    if (safe(r.mobile).isBlank()) r.mobile = value;
                    else r.phone = value;
                    changed = true;
                    break;
                case "EMAIL":
                    r.email = value;
                    changed = true;
                    break;
                case "URL":
                    r.website = value;
                    changed = true;
                    break;
                case "ADR":
                    r.address = value;
                    changed = true;
                    break;
                case "NOTE":
                    r.notes = append(r.notes, value);
                    break;
                default:
                    break;
            }
        }
        return changed;
    }

    private static boolean isUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") ||
                lower.startsWith("www.") || lower.contains("linkedin.com/");
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return value;
        }
    }

    private static String unescape(String value) {
        return value.replace("\\n", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }

    private static String addTag(String tags, String tag) {
        String current = safe(tags).trim();
        if (current.isEmpty()) return tag;
        for (String part : current.split(",")) {
            if (part.trim().equalsIgnoreCase(tag)) return current;
        }
        return current + ", " + tag;
    }

    private static String append(String existing, String line) {
        String a = safe(existing).trim();
        String b = safe(line).trim();
        if (b.isEmpty()) return a;
        if (a.isEmpty()) return b;
        return a + "\n" + b;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
