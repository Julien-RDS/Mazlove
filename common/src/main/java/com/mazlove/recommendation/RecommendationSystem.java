package com.mazlove.recommendation;

import java.util.*;
import java.util.stream.Collectors;

public class RecommendationSystem {
    private static final int DEFAULT_K = 5;
    private static final double DEFAULT_ELO_K_FACTOR = 32.0;
    private static final double DEFAULT_TASTE_WEIGHT = 0.4;
    private static final double DEFAULT_SIMILARITY_WEIGHT = 0.3;
    private static final double DEFAULT_ELO_WEIGHT = 0.3;

    private final int k;
    private final double eloKFactor;
    private final double tasteWeight;
    private final double similarityWeight;
    private final double eloWeight;

    private final Map<String, Item> items;
    private final Map<String, User> users;
    private final List<Interaction> interactions;

    public RecommendationSystem(int k, double eloKFactor, double tasteWeight, double similarityWeight, double eloWeight) {
        this.k = k;
        this.eloKFactor = eloKFactor;
        this.tasteWeight = tasteWeight;
        this.similarityWeight = similarityWeight;
        this.eloWeight = eloWeight;
        
        this.items = new HashMap<>();
        this.users = new HashMap<>();
        this.interactions = new ArrayList<>();
    }

    public RecommendationSystem() {
        this(DEFAULT_K, DEFAULT_ELO_K_FACTOR, DEFAULT_TASTE_WEIGHT, DEFAULT_SIMILARITY_WEIGHT, DEFAULT_ELO_WEIGHT);
    }

    public void addItem(String itemId, double[] features, int initialElo) {
        items.put(itemId, new Item(itemId, features, initialElo));
    }

    public void addUser(String userId, Map<String, Double> tasteProfile) {
        users.put(userId, new User(userId, tasteProfile));
    }

    public void recordInteraction(String userId, String itemId, double rating) {
        if (!users.containsKey(userId) || !items.containsKey(itemId)) {
            return;
        }

        Interaction interaction = new Interaction(userId, itemId, rating);
        interactions.add(interaction);
        users.get(userId).addRating(itemId, rating);
        
        updateEloScore(itemId, rating);
        updateSimilarUsers(userId);
    }

    private void updateEloScore(String itemId, double rating) {
        Item item = items.get(itemId);
        double currentElo = item.getElo();
        double expectedScore = 0.5;
        
        double newElo = currentElo + eloKFactor * (rating - expectedScore);
        item.setElo(Math.max(0, newElo));
        item.incrementInteractionCount();
    }

    private void updateSimilarUsers(String userId) {
        if (users.size() < 2) {
            return;
        }

        User currentUser = users.get(userId);
        List<SimilarUser> similarUsers = new ArrayList<>();

        for (Map.Entry<String, User> entry : users.entrySet()) {
            String otherUserId = entry.getKey();
            if (otherUserId.equals(userId)) {
                continue;
            }

            User otherUser = entry.getValue();
            double tasteSim = calculateTasteSimilarity(currentUser.getTasteProfile(), otherUser.getTasteProfile());
            double ratingSim = calculateRatingSimilarity(currentUser.getRatings(), otherUser.getRatings());
            
            double combinedSim = 0.6 * tasteSim + 0.4 * ratingSim;
            similarUsers.add(new SimilarUser(otherUserId, combinedSim));
        }

        similarUsers.sort(Comparator.comparingDouble(SimilarUser::getSimilarity).reversed());
        currentUser.setSimilarUsers(similarUsers.subList(0, Math.min(k, similarUsers.size())));
    }

    private double calculateTasteSimilarity(Map<String, Double> profile1, Map<String, Double> profile2) {
        if (profile1 == null || profile2 == null) {
            return 0.0;
        }

        Set<String> commonKeys = new HashSet<>(profile1.keySet());
        commonKeys.retainAll(profile2.keySet());
        
        if (commonKeys.isEmpty()) {
            return 0.0;
        }

        double[] vec1 = commonKeys.stream()
            .mapToDouble(profile1::get)
            .toArray();
        
        double[] vec2 = commonKeys.stream()
            .mapToDouble(profile2::get)
            .toArray();

        return cosineSimilarity(vec1, vec2);
    }

    private double calculateRatingSimilarity(Map<String, Double> ratings1, Map<String, Double> ratings2) {
        if (ratings1 == null || ratings2 == null) {
            return 0.0;
        }

        Set<String> commonItems = new HashSet<>(ratings1.keySet());
        commonItems.retainAll(ratings2.keySet());
        
        if (commonItems.size() < 2) {
            return 0.0;
        }

        double[] ratings1Common = commonItems.stream()
            .mapToDouble(ratings1::get)
            .toArray();
        
        double[] ratings2Common = commonItems.stream()
            .mapToDouble(ratings2::get)
            .toArray();

        return pearsonCorrelation(ratings1Common, ratings2Common);
    }

    private double cosineSimilarity(double[] vec1, double[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += Math.pow(vec1[i], 2);
            norm2 += Math.pow(vec2[i], 2);
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0 ? 0 : dotProduct / denominator;
    }

    private double pearsonCorrelation(double[] x, double[] y) {
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;
        double sumY2 = 0;

        for (int i = 0; i < x.length; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += Math.pow(x[i], 2);
            sumY2 += Math.pow(y[i], 2);
        }

        double numerator = x.length * sumXY - sumX * sumY;
        double denominator = Math.sqrt((x.length * sumX2 - Math.pow(sumX, 2)) * (x.length * sumY2 - Math.pow(sumY, 2)));

        return denominator == 0 ? 0 : numerator / denominator;
    }

    public List<Recommendation> recommend(String userId, int nRecommendations) {
        if (!users.containsKey(userId)) {
            return Collections.emptyList();
        }

        User user = users.get(userId);
        List<Recommendation> recommendations = new ArrayList<>();

        for (Map.Entry<String, Item> entry : items.entrySet()) {
            String itemId = entry.getKey();
            if (user.hasRated(itemId)) {
                continue;
            }

            double predictedRating = predictRating(userId, itemId);
            recommendations.add(new Recommendation(itemId, predictedRating));
        }

        return recommendations.stream()
            .sorted(Comparator.comparingDouble(Recommendation::getScore).reversed())
            .limit(nRecommendations)
            .collect(Collectors.toList());
    }

    private double predictRating(String userId, String itemId) {
        User user = users.get(userId);
        Item item = items.get(itemId);

        double tasteScore = calculateTasteCompatibility(user.getTasteProfile(), item.getFeatures());
        double eloScore = normalizeElo(item.getElo());
        double knnScore = calculateKnnScore(userId, itemId);

        return Math.min(1.0, Math.max(0.0, 
            tasteWeight * tasteScore +
            eloWeight * eloScore +
            similarityWeight * knnScore));
    }

    private double calculateTasteCompatibility(Map<String, Double> profile, double[] features) {
        if (profile == null || features == null) {
            return 0.5;
        }

        double[] profileVector = profile.values().stream()
            .mapToDouble(Double::doubleValue)
            .toArray();

        if (profileVector.length != features.length) {
            return 0.5;
        }

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (int i = 0; i < profileVector.length; i++) {
            dotProduct += profileVector[i] * features[i];
            norm1 += Math.pow(profileVector[i], 2);
            norm2 += Math.pow(features[i], 2);
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0 ? 0.5 : Math.max(0, dotProduct / denominator);
    }

    private double normalizeElo(double elo) {
        return Math.min(1.0, Math.max(0.0, (elo - 800) / 800));
    }

    private double calculateKnnScore(String userId, String itemId) {
        User user = users.get(userId);
        List<SimilarUser> similarUsers = user.getSimilarUsers();
        
        if (similarUsers.isEmpty()) {
            return 0.5;
        }

        double weightedSum = 0;
        double similaritySum = 0;

        for (SimilarUser similarUser : similarUsers) {
            String similarUserId = similarUser.getUserId();
            User otherUser = users.get(similarUserId);
            
            if (otherUser.hasRated(itemId)) {
                double rating = otherUser.getRating(itemId);
                double similarity = similarUser.getSimilarity();
                
                weightedSum += similarity * rating;
                similaritySum += similarity;
            }
        }

        return similaritySum == 0 ? 0.5 : weightedSum / similaritySum;
    }

    public Map<String, Object> getSystemStats() {
        double avgElo = items.values().stream()
            .mapToDouble(Item::getElo)
            .average()
            .orElse(0.0);

        return Map.of(
            "totalUsers", users.size(),
            "totalItems", items.size(),
            "totalInteractions", interactions.size(),
            "avgElo", avgElo,
            "weights", Map.of(
                "taste", tasteWeight,
                "similarity", similarityWeight,
                "elo", eloWeight
            )
        );
    }

    // Inner classes
    private static class Item {
        private final String id;
        private double elo;
        private final double[] features;
        private int interactionCount;

        public Item(String id, double[] features, double elo) {
            this.id = id;
            this.elo = elo;
            this.features = features;
            this.interactionCount = 0;
        }

        public String getId() { return id; }
        public double getElo() { return elo; }
        public void setElo(double elo) { this.elo = elo; }
        public double[] getFeatures() { return features; }
        public int getInteractionCount() { return interactionCount; }
        public void incrementInteractionCount() { interactionCount++; }
    }

    private static class User {
        private final String id;
        private final Map<String, Double> tasteProfile;
        private final Map<String, Double> ratings;
        private List<SimilarUser> similarUsers;

        public User(String id, Map<String, Double> tasteProfile) {
            this.id = id;
            this.tasteProfile = tasteProfile;
            this.ratings = new HashMap<>();
            this.similarUsers = new ArrayList<>();
        }

        public String getId() { return id; }
        public Map<String, Double> getTasteProfile() { return tasteProfile; }
        public Map<String, Double> getRatings() { return ratings; }
        public List<SimilarUser> getSimilarUsers() { return similarUsers; }
        public void setSimilarUsers(List<SimilarUser> similarUsers) { this.similarUsers = similarUsers; }
        public boolean hasRated(String itemId) { return ratings.containsKey(itemId); }
        public double getRating(String itemId) { return ratings.getOrDefault(itemId, 0.0); }
        public void addRating(String itemId, double rating) { ratings.put(itemId, rating); }
    }

    private static class SimilarUser {
        private final String userId;
        private final double similarity;

        public SimilarUser(String userId, double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }

        public String getUserId() { return userId; }
        public double getSimilarity() { return similarity; }
    }

    private static class Interaction {
        private final String userId;
        private final String itemId;
        private final double rating;

        public Interaction(String userId, String itemId, double rating) {
            this.userId = userId;
            this.itemId = itemId;
            this.rating = rating;
        }
    }

    private static class Recommendation {
        private final String itemId;
        private final double score;

        public Recommendation(String itemId, double score) {
            this.itemId = itemId;
            this.score = score;
        }

        public String getItemId() { return itemId; }
        public double getScore() { return score; }
    }
}
