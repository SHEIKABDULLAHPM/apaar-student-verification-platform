package com.apaar.platform.identity;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Transparent, deterministic string similarity utility for student identity attributes.
 * Implements normalized Levenshtein distance and token-sorted comparison for names.
 */
public final class StringSimilarityUtil {

    private StringSimilarityUtil() {
    }

    /**
     * Compute normalized similarity between two strings in range [0.0, 1.0].
     * 1.0 indicates exact match (case and whitespace normalized).
     */
    public static double nameSimilarity(String s1, String s2) {
        if (s1 == null && s2 == null) return 1.0;
        if (s1 == null || s2 == null) return 0.0;

        String n1 = normalizeName(s1);
        String n2 = normalizeName(s2);

        if (n1.isEmpty() && n2.isEmpty()) return 1.0;
        if (n1.isEmpty() || n2.isEmpty()) return 0.0;
        if (n1.equals(n2)) return 1.0;

        // Token sorted match (e.g. "Aarav Sharma" vs "Sharma, Aarav")
        String t1 = tokenSort(n1);
        String t2 = tokenSort(n2);
        if (t1.equals(t2)) return 0.98;

        // Levenshtein ratio
        int maxLen = Math.max(n1.length(), n2.length());
        int dist = levenshteinDistance(n1, n2);
        double levSim = 1.0 - ((double) dist / (double) maxLen);

        // Also check token-sorted distance
        int tokenDist = levenshteinDistance(t1, t2);
        double tokenSim = 1.0 - ((double) tokenDist / (double) Math.max(t1.length(), t2.length()));

        return Math.max(levSim, tokenSim);
    }

    public static int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    private static String normalizeName(String s) {
        return s.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String tokenSort(String s) {
        String[] tokens = s.split("\\s+");
        Arrays.sort(tokens);
        return String.join(" ", tokens);
    }
}

