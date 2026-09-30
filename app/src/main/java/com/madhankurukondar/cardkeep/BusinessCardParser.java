package com.madhankurukondar.cardkeep;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BusinessCardParser {
    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL = Pattern.compile("(?:https?://)?(?:www\\.)?[A-Z0-9.-]+\\.[A-Z]{2,}(?:/[^\\s]*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(?:\\+?\\d[\\d ()/.-]{6,}\\d)");

    private static final String[] TITLE_WORDS = {
            "director", "manager", "engineer", "president", "founder", "owner",
            "ceo", "cto", "cfo", "coo", "vice president", "vp", "head", "lead",
            "sales", "business development", "architect", "consultant", "specialist"
    };

    private static final String[] COMPANY_WORDS = {
            "gmbh", " ag", "inc", "llc", "ltd", "limited", "corp", "corporation",
            "technologies", "technology", "systems", "solutions", "semiconductor",
            "group", "s.a.", "s.r.l", " oy", " ab"
    };

    private BusinessCardParser() {}

    public static ContactRecord parse(String raw) {
        ContactRecord r = new ContactRecord();
        r.rawText = raw == null ? "" : raw;

        List<String> lines = cleanLines(r.rawText);
        List<String> emails = matches(EMAIL, r.rawText);
        List<String> urls = matches(URL, r.rawText);
        urls.removeIf(s -> s.contains("@"));

        List<String> phoneLines = new ArrayList<>();
        List<String> phones = new ArrayList<>();
        for (String line : lines) {
            Matcher m = PHONE.matcher(line);
            if (m.find()) {
                phoneLines.add(line);
                phones.add(m.group().trim());
            }
        }

        List<String> textLines = new ArrayList<>();
        for (String line : lines) {
            if (!EMAIL.matcher(line).find() && !URL.matcher(line).find() && !PHONE.matcher(line).find()) {
                textLines.add(line);
            }
        }

        r.company = firstMatching(textLines, BusinessCardParser::looksLikeCompany);
        r.title = firstMatching(textLines, BusinessCardParser::looksLikeTitle);

        for (String line : textLines) {
            if (line.equals(r.company) || line.equals(r.title)) continue;
            if (looksLikeName(line)) {
                r.name = line;
                break;
            }
        }

        r.email = emails.isEmpty() ? "" : emails.get(0);
        r.website = urls.isEmpty() ? "" : trimPunctuation(urls.get(0));

        int mobileIndex = -1;
        for (int i = 0; i < phoneLines.size(); i++) {
            String lower = phoneLines.get(i).toLowerCase(Locale.ROOT);
            if (lower.contains("mobile") || lower.contains("mob") || lower.contains("cell") || lower.contains("m:")) {
                mobileIndex = i;
                break;
            }
        }
        if (mobileIndex >= 0 && mobileIndex < phones.size()) r.mobile = phones.get(mobileIndex);
        else if (phones.size() > 1) r.mobile = phones.get(1);

        for (String p : phones) {
            if (!p.equals(r.mobile)) {
                r.phone = p;
                break;
            }
        }

        List<String> address = new ArrayList<>();
        for (String line : textLines) {
            if (line.equals(r.name) || line.equals(r.company) || line.equals(r.title)) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            if (containsDigit(line) || lower.matches(".*\\b(street|strasse|straße|road|avenue|ave|boulevard|blvd|platz|weg|lane|drive)\\b.*")) {
                address.add(line);
                if (address.size() == 3) break;
            }
        }
        r.address = String.join(", ", address);
        return r;
    }

    private interface LinePredicate { boolean test(String line); }

    private static String firstMatching(List<String> lines, LinePredicate predicate) {
        for (String line : lines) if (predicate.test(line)) return line;
        return "";
    }

    private static boolean looksLikeCompany(String line) {
        String lower = " " + line.toLowerCase(Locale.ROOT) + " ";
        for (String word : COMPANY_WORDS) if (lower.contains(word)) return true;
        return line.length() >= 3 && line.length() <= 36 && line.equals(line.toUpperCase(Locale.ROOT)) && containsLetter(line);
    }

    private static boolean looksLikeTitle(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        for (String word : TITLE_WORDS) if (lower.contains(word)) return true;
        return false;
    }

    private static boolean looksLikeName(String line) {
        if (containsDigit(line) || line.length() > 60) return false;
        String[] words = line.replace("|", " ").replace("•", " ").trim().split("\\s+");
        return words.length >= 2 && words.length <= 5 && containsLetter(line);
    }

    private static List<String> cleanLines(String raw) {
        Set<String> unique = new LinkedHashSet<>();
        for (String line : raw.split("\\r?\\n")) {
            String x = line.trim().replaceAll("\\s{2,}", " ");
            if (!x.isEmpty()) unique.add(x);
        }
        return new ArrayList<>(unique);
    }

    private static List<String> matches(Pattern p, String raw) {
        Set<String> unique = new LinkedHashSet<>();
        Matcher m = p.matcher(raw);
        while (m.find()) unique.add(m.group().trim());
        return new ArrayList<>(unique);
    }

    private static String trimPunctuation(String s) {
        return s.replaceAll("[.,;]+$", "");
    }

    private static boolean containsDigit(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isDigit(s.charAt(i))) return true;
        return false;
    }

    private static boolean containsLetter(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i))) return true;
        return false;
    }
}
