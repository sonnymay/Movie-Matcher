package com.yourname.moviematcher.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yourname.moviematcher.model.Movie;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieAPIClient movieAPIClient;
    private List<Movie> allMovies = new ArrayList<>();
    private List<Movie> userChoices = new ArrayList<>();
    private Set<String> skippedMovieIds = new HashSet<>(); // Track skipped movie IDs
    private Random random = new Random();
    
    // Track genre preferences
    private Map<String, Integer> genrePreferences = new HashMap<>();
    
    // Number of movies to keep as candidates for matching
    private static final int CANDIDATE_POOL_SIZE = 50;

    @Autowired
    public MovieServiceImpl(MovieAPIClient movieAPIClient) {
        this.movieAPIClient = movieAPIClient;
    }

    @Override
    public List<Movie> getPopularMovies() {
        return movieAPIClient.getPopularMovies();
    }

    @Override
    public List<Movie> getPopularTvShows() {
        return movieAPIClient.getPopularTvShows();
    }

    @Override
    public List<Movie> getAllContent() {
        if (allMovies.isEmpty()) {
            refreshMovies();
        }
        return allMovies;
    }

    @Override
    public void refreshMovies() {
        List<Movie> movies = new ArrayList<>();
        movies.addAll(movieAPIClient.getPopularMovies());
        movies.addAll(movieAPIClient.getPopularTvShows());
        allMovies = movies;
    }

    @Override
    public Movie getMovieDetails(String id) {
        return movieAPIClient.getMovieDetails(id);
    }

    @Override
    public List<Movie> getNextChoices(String platform) {
        try {
            // First check if we need to refresh our content based on platform
            if ("All".equals(platform)) {
                // Just use what we have or refresh if empty
                if (allMovies.isEmpty()) {
                    refreshMovies();
                }
            } else {
                // Get content specific to this platform
                List<Movie> platformContent = movieAPIClient.getContentByPlatform(platform);
                if (!platformContent.isEmpty()) {
                    // Replace our content with platform-specific content
                    allMovies = platformContent;
                }
            }

            // Filter out movies already chosen AND skipped movies
            List<Movie> filteredMovies = allMovies.stream()
                    .filter(movie -> !userChoices.contains(movie) && !skippedMovieIds.contains(movie.getId()))
                    .collect(Collectors.toList());

            // If we don't have enough movies to choose from, reset choices or refresh from API
            if (filteredMovies.size() < 2) {
                // First try refreshing from API
                refreshMovies();
                filteredMovies = allMovies.stream()
                        .filter(movie -> !userChoices.contains(movie) && !skippedMovieIds.contains(movie.getId()))
                        .collect(Collectors.toList());

                // If still not enough, just reset user choices but keep skipped movies
                if (filteredMovies.size() < 2) {
                    userChoices.clear();
                    filteredMovies = allMovies.stream()
                            .filter(movie -> !skippedMovieIds.contains(movie.getId()))
                            .collect(Collectors.toList());
                    
                    // If we still don't have enough, reset everything
                    if (filteredMovies.size() < 2) {
                        skippedMovieIds.clear();
                        filteredMovies = new ArrayList<>(allMovies);
                    }
                }
            }
            
            // Select two movies using weighted selection based on preferences
            List<Movie> candidatePool = getCandidateMoviePool(filteredMovies);
            List<Movie> choices = selectMoviesBasedOnPreferences(candidatePool);
            
            return choices;
        } catch (Exception e) {
            System.err.println("Error getting next choices: " + e.getMessage());
            e.printStackTrace();

            // Return any two movies as backup
            List<Movie> fallbackChoices = new ArrayList<>();
            for (int i = 0; i < Math.min(2, allMovies.size()); i++) {
                fallbackChoices.add(allMovies.get(i));
            }
            return fallbackChoices;
        }
    }
    
    /**
     * Gets a candidate pool of movies to select from based on ratings and diversity
     */
    private List<Movie> getCandidateMoviePool(List<Movie> filteredMovies) {
        // Sort by rating descending
        List<Movie> sortedByRating = filteredMovies.stream()
                .sorted(Comparator.comparing(Movie::getRating).reversed())
                .collect(Collectors.toList());
        
        // Take top-rated movies
        int poolSize = Math.min(CANDIDATE_POOL_SIZE, sortedByRating.size());
        List<Movie> topRated = sortedByRating.subList(0, poolSize);
        
        // If we have user preferences, add some recommended movies based on genres
        if (!userChoices.isEmpty() && !genrePreferences.isEmpty()) {
            // Get the top 3 preferred genres
            final List<String> preferredGenres = genrePreferences.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            // Find movies matching these genres that aren't in top-rated
            Set<Movie> topRatedSet = new HashSet<>(topRated);
            List<Movie> genreMatches = filteredMovies.stream()
                    .filter(movie -> !topRatedSet.contains(movie))
                    .filter(movie -> {
                        for (String genre : preferredGenres) {
                            if (movie.getGenres().contains(genre)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .limit(10) // Add up to 10 genre-matched movies
                    .collect(Collectors.toList());
            
            // Add genre matches to candidate pool
            topRated.addAll(genreMatches);
        }
        
        return topRated;
    }
    
    /**
     * Select two movies based on user preferences and rating
     */
    private List<Movie> selectMoviesBasedOnPreferences(List<Movie> candidatePool) {
        List<Movie> choices = new ArrayList<>();
        
        // If this is the first choice or we have no preferences yet, just pick randomly from top-rated
        if (userChoices.isEmpty() || genrePreferences.isEmpty()) {
            // Pick two random movies from the candidate pool
            int firstIndex = random.nextInt(candidatePool.size());
            choices.add(candidatePool.get(firstIndex));
            candidatePool.remove(firstIndex);
            
            int secondIndex = random.nextInt(candidatePool.size());
            choices.add(candidatePool.get(secondIndex));
            
            return choices;
        }
        
        // Otherwise, use weighted selection based on genre preferences
        
        // Calculate total genre weight
        int totalWeight = genrePreferences.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight == 0) totalWeight = 1; // Avoid division by zero
        
        // Assign weights to each movie based on genres
        final int finalTotalWeight = totalWeight; // Make effectively final for lambda
        Map<Movie, Double> movieWeights = new HashMap<>();
        for (Movie movie : candidatePool) {
            double weight = calculateMovieWeight(movie, finalTotalWeight);
            movieWeights.put(movie, weight);
        }
        
        // Sort movies by weight descending
        List<Movie> weightedMovies = candidatePool.stream()
                .sorted(Comparator.comparing(movie -> movieWeights.getOrDefault(movie, 0.0)).reversed())
                .collect(Collectors.toList());
        
        // First pick a movie that matches preferences well
        choices.add(weightedMovies.get(0));
        weightedMovies.remove(0);
        
        // For second pick, either:
        // 1. 70% chance: pick another movie matching preferences
        // 2. 30% chance: pick a random movie for diversity
        if (random.nextDouble() < 0.7) {
            choices.add(weightedMovies.get(0)); // Get the next highest weighted movie
        } else {
            // Pick a random movie from the rest
            int randomIndex = random.nextInt(weightedMovies.size());
            choices.add(weightedMovies.get(randomIndex));
        }
        
        return choices;
    }
    
    /**
     * Calculate a weight for a movie based on how well it matches genre preferences
     */
    private double calculateMovieWeight(Movie movie, int totalWeight) {
        // Base weight from rating (0-10 scale)
        double weight = movie.getRating() / 2.0; // Convert to 0-5 scale
        
        // Add weight based on genre matches
        for (String genre : movie.getGenres()) {
            if (genrePreferences.containsKey(genre)) {
                weight += (genrePreferences.get(genre) * 5.0) / totalWeight;
            }
        }
        
        return weight;
    }
    
    @Override
    public void recordChoice(String chosenId) {
        // Find the chosen movie
        Movie chosenMovie = allMovies.stream()
                .filter(movie -> movie.getId().equals(chosenId))
                .findFirst()
                .orElse(null);
        
        if (chosenMovie != null) {
            // Add to user choices
            userChoices.add(chosenMovie);
            
            // Update genre preferences
            for (String genre : chosenMovie.getGenres()) {
                genrePreferences.put(genre, genrePreferences.getOrDefault(genre, 0) + 1);
            }
        }
    }
    
    @Override
    public void skipCurrentPair(String leftId, String rightId) {
        // Add both movie IDs to the skipped list
        if (leftId != null) {
            skippedMovieIds.add(leftId);
            System.out.println("Added " + leftId + " to skipped movies list");
        }
        
        if (rightId != null) {
            skippedMovieIds.add(rightId);
            System.out.println("Added " + rightId + " to skipped movies list");
        }
    }

    @Override
    public Movie getFinalRecommendation() {
        // If we have choices, find the best match based on preferences
        if (!userChoices.isEmpty() && !genrePreferences.isEmpty()) {
            // Remove chosen movies and skipped movies
            List<Movie> candidates = allMovies.stream()
                    .filter(movie -> !userChoices.contains(movie) && !skippedMovieIds.contains(movie.getId()))
                    .collect(Collectors.toList());
            
            // Calculate weights for all movies
            int totalWeight = genrePreferences.values().stream().mapToInt(Integer::intValue).sum();
            if (totalWeight == 0) totalWeight = 1; // Avoid division by zero
            
            // Find movie with highest match score
            final int finalTotalWeight = totalWeight; // Make effectively final for lambda
            Movie bestMatch = candidates.stream()
                    .max(Comparator.comparing(movie -> calculateMovieWeight(movie, finalTotalWeight)))
                    .orElse(null);
            
            System.out.println("Found best match: " + (bestMatch != null ? bestMatch.getTitle() : "none"));
            System.out.println("Number of skipped movies: " + skippedMovieIds.size());
            
            if (bestMatch != null) {
                return bestMatch;
            }
        }
        
        // Fallback: return last chosen movie or random one that wasn't skipped
        if (userChoices.isEmpty()) {
            // Filter out skipped movies
            List<Movie> availableMovies = allMovies.stream()
                    .filter(movie -> !skippedMovieIds.contains(movie.getId()))
                    .collect(Collectors.toList());
            
            // If no available movies, use all movies
            if (availableMovies.isEmpty()) {
                availableMovies = allMovies;
            }
            
            return availableMovies.get(random.nextInt(availableMovies.size()));
        }
        
        return userChoices.get(userChoices.size() - 1);
    }

    @Override
    public void resetChoices() {
        userChoices.clear();
        skippedMovieIds.clear();
        genrePreferences.clear();
    }

    @Override
    public List<Movie> getMatchRecommendations(String movie1Id, String movie2Id) {
        // Get recommendations based on both movies
        List<Movie> movie1Recs = movieAPIClient.getRecommendations(movie1Id);
        List<Movie> movie2Recs = movieAPIClient.getRecommendations(movie2Id);
        
        // Combine recommendations, prioritizing movies that appear in both lists
        Map<String, Movie> allRecsMap = new HashMap<>();
        Map<String, Integer> recScores = new HashMap<>();
        
        // Add first movie recommendations with score 1
        for (Movie movie : movie1Recs) {
            // Skip movies that user has already skipped
            if (skippedMovieIds.contains(movie.getId())) {
                continue;
            }
            
            allRecsMap.put(movie.getId(), movie);
            recScores.put(movie.getId(), 1);
        }
        
        // Add second movie recommendations, incrementing score if already present
        for (Movie movie : movie2Recs) {
            // Skip movies that user has already skipped
            if (skippedMovieIds.contains(movie.getId())) {
                continue;
            }
            
            allRecsMap.put(movie.getId(), movie);
            recScores.put(movie.getId(), recScores.getOrDefault(movie.getId(), 0) + 1);
        }
        
        // Sort by score (items in both lists first) then by rating
        List<Movie> sortedRecs = allRecsMap.values().stream()
                .sorted(Comparator
                        .comparing((Movie m) -> recScores.get(m.getId())).reversed()
                        .thenComparing(Movie::getRating, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        
        // Return top 10 recommendations
        return sortedRecs.stream().limit(10).collect(Collectors.toList());
    }
}