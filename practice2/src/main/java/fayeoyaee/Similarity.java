package fayeoyaee;

import java.util.*;

public class Similarity {
    /**
     * return Jaccard index
     */
    public static double getJaccard(String s1, String s2) {
        if ((s1.isEmpty() || s1 == null) && (s2.isEmpty() || s2 == null)) return 0;
        
        Set<Character> set1 = new HashSet<>();
        s1.chars().mapToObj(c -> (char) c).forEach(c -> set1.add(c));;

        Set<Character> unique = new HashSet<>(); 
        Set<Character> both = new HashSet<>(set1);
        s2.chars().mapToObj(c -> (char) c).forEach(c -> {if (set1.contains(c)) unique.add(c); else both.add(c);});

		return unique.size() * 1.0 / both.size();
    }
    
    /**
     * returns Levenshtein distance (edit distance)
     */
    public static int getLevenshtein(String s1, String s2) {
        int m = s1.length(); 
        int n = s2.length();
        int[][] dp = new int[m+1][n+1];

        dp[0][0] = 0;
        for (int i = 1; i < m+1; ++i) {
            dp[i][0] = i;
        }
        for (int j = 0; j < n+1; ++j) {
            dp[0][j] = j;
        }
        for (int i = 1; i < m+1; i++) {
            for (int j = 1; j < n+1; j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i-1][j], dp[i][j-1]), dp[i-1][j-1]);
                }
            }
        }
		return dp[m][n];
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose a similarity method (Jaccard = 1, Levenshtein = 2): ");
        String method = scanner.nextLine();
        System.out.println("Enter the first string: ");
        String s1 = scanner.nextLine();
        System.out.println("Enter the second string: ");
        String s2 = scanner.nextLine();
        switch (method) {
            case "1" : 
                System.out.printf("Jaccard index between %s and %s is %.2f\n", s1, s2, getJaccard(s1, s2));
                break;
            case "2" : 
                System.out.printf("Levenshtein distance between %s and %s is %d\n", s1, s2, getLevenshtein(s1, s2));
                break;
            default : 
                System.out.printf("Invalid input %s %s, %s\n", method, s1, s2);
                break;
        }
        scanner.close();
    }
}