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
            "sales", "business development", "architect", "consultant", "specialist",
            "marketing", "applications", "application", "product", "technical"
    };

    private static final String[] COMPANY_WORDS = {
            "gmbh", " ag", "inc", "llc", "ltd", "limited", "corp", "corporation",
            "incorporated", "technologies", "technology", "systems", "solutions",
            "semiconductor", "group", "s.a.", "s.r.l", " oy", " ab"
    };

    private static final String[] NON_NAME_WORDS = {
            "embedded", "processor", "processors", "software", "hardware", "product",
            "marketing", "technical", "applications", "application", "engineering",
            "sales", "business", "development", "department", "division", "team",
            "systems", "solutions", "technologies", "technology", "semiconductor",
            "incorporated", "corporation", "company", "canada", "usa"
    };

    private BusinessCardParser() {}

    public static ContactRecord parse(String raw) {
        ContactRecord r = new ContactRecord();
        r.rawText = raw == null ? "" : raw;

        String normalizedRaw = normalizeOcrPunctuation(r.rawText);
        List<String> lines = cleanLines(normalizedRaw);
        List<String> emails = extractEmails(lines, normalizedRaw);
        List<String> urls = matches(URL, normalizedRaw);
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
            if (!containsEmail(line) && !URL.matcher(line).find() && !PHONE.matcher(line).find()) {
                textLines.add(line);
            }
        }

        r.company = firstMatching(textLines, BusinessCardParser::looksLikeCompany);
        r.title = firstMatching(textLines, BusinessCardParser::looksLikeTitle);
        r.name = bestNameCandidate(textLines, r.company, r.title, emails);

        r.email = emails.isEmpty() ? "" : trimPunctuation(emails.get(0));
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
        return line.length() >= 2 && line.length() <= 36
                && line.equals(line.toUpperCase(Locale.ROOT)) && containsLetter(line);
    }

    private static boolean looksLikeTitle(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        for (String word : TITLE_WORDS) if (lower.contains(word)) return true;
        return false;
    }

    private static String bestNameCandidate(List<String> lines,
                                            String company,
                                            String title,
                                            List<String> emails) {
        String best = "";
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.equals(company) || line.equals(title)) continue;

            int score = nameScore(line, i, emails);
            if (score > bestScore) {
                bestScore = score;
                best = line;
            }
        }
        return bestScore >= 4 ? best : "";
    }

    private static int nameScore(String line, int lineIndex, List<String> emails) {
        if (line == null || line.isEmpty() || containsDigit(line) || line.length() > 60) {
            return Integer.MIN_VALUE;
        }
        if (looksLikeTitle(line) || looksLikeCompany(line)) return Integer.MIN_VALUE;
        if (line.contains("@") || line.contains("://")) return Integer.MIN_VALUE;

        String cleaned = line.replace("|", " ").replace("•", " ").trim();
        String[] words = cleaned.split("\\s+");
        if (words.length < 2 || words.length > 5) return Integer.MIN_VALUE;

        String lower = cleaned.toLowerCase(Locale.ROOT);
        for (String word : NON_NAME_WORDS) {
            if (containsWord(lower, word)) return -10;
        }
        if (lower.matches(".*\\b(street|strasse|straße|road|avenue|ave|boulevard|blvd|lane|drive|ottawa|dallas)\\b.*")) {
            return -10;
        }

        int score = 4;
        if (words.length == 2 || words.length == 3) score += 2;
        if (lineIndex == 0) score += 3;
        else if (lineIndex <= 2) score += 2;
        else if (lineIndex <= 4) score += 1;

        int nameLikeWords = 0;
        for (String word : words) {
            String token = word.replaceAll("^[^\\p{L}]+|[^\\p{L}'’-]+$", "");
            if (token.isEmpty()) continue;
            if (Character.isUpperCase(token.codePointAt(0)) || token.equals(token.toUpperCase(Locale.ROOT))) {
                nameLikeWords++;
            }
        }
        if (nameLikeWords == words.length) score += 2;
        else if (nameLikeWords >= Math.max(1, words.length - 1)) score += 1;

        if (line.contains(",") || line.contains(":")) score -= 3;

        String surname = normalizeLetters(words[words.length - 1]);
        if (!surname.isEmpty()) {
            for (String email : emails) {
                int at = email.indexOf('@');
                if (at <= 0) continue;
                String local = normalizeLetters(email.substring(0, at));
                if (local.contains(surname)) {
                    score += 4;
                    break;
                }
            }
        }

        return score;
    }

    private static boolean containsWord(String lowerLine, String lowerWord) {
        String[] tokens = lowerLine.split("[^\\p{L}]+");
        for (String token : tokens) {
            if (token.equals(lowerWord)) return true;
        }
        return false;
    }

    private static String normalizeLetters(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static String normalizeOcrPunctuation(String raw) {
        return raw
                .replace('\u2010', '-')
                .replace('\u2011', '-')
                .replace('\u2012', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2212', '-');
    }

    private static List<String> extractEmails(List<String> lines, String raw) {
        Set<String> unique = new LinkedHashSet<>(matches(EMAIL, raw));

        for (String line : lines) {
            if (!line.contains("@")) continue;
            String compact = line
                    .replaceAll("\\s*@\\s*", "@")
                    .replaceAll("\\s*([._%+\\-])\\s*", "$1");
            Matcher m = EMAIL.matcher(compact);
            while (m.find()) unique.add(m.group().trim());
        }
        return new ArrayList<>(unique);
    }

    private static boolean containsEmail(String line) {
        if (EMAIL.matcher(line).find()) return true;
        if (!line.contains("@")) return false;
        String compact = line
                .replaceAll("\\s*@\\s*", "@")
                .replaceAll("\\s*([._%+\\-])\\s*", "$1");
        return EMAIL.matcher(compact).find();
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
