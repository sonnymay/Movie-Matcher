package com.yourname.moviematcher.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private Random random = new Random();

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

            // Filter out movies already chosen
            List<Movie> filteredMovies = new ArrayList<>(allMovies);
            filteredMovies.removeAll(userChoices);

            // If we don't have enough movies to choose from, reset choices or refresh from API
            if (filteredMovies.size() < 2) {
                // First try refreshing from API
                refreshMovies();
                filteredMovies = new ArrayList<>(allMovies);
                filteredMovies.removeAll(userChoices);

                // If still not enough, just reset user choices
                if (filteredMovies.size() < 2) {
                    userChoices.clear();
                    filteredMovies = new ArrayList<>(allMovies);
                }
            }

            // Randomly select two movies
            List<Movie> choices = new ArrayList<>();
            int index1 = random.nextInt(filteredMovies.size());
            choices.add(filteredMovies.get(index1));
            filteredMovies.remove(index1);

            int index2 = random.nextInt(filteredMovies.size());
            choices.add(filteredMovies.get(index2));

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

    @Override
    public void recordChoice(String chosenId) {
        allMovies.stream()
                .filter(movie -> movie.getId().equals(chosenId))
                .findFirst()
                .ifPresent(userChoices::add);
    }

    @Override
    public Movie getFinalRecommendation() {
        // Simple implementation: return the last chosen movie
        // In a real app, you could use a more sophisticated algorithm
        if (userChoices.isEmpty()) {
            return allMovies.get(random.nextInt(allMovies.size()));
        }
        return userChoices.get(userChoices.size() - 1);
    }

    @Override
    public void resetChoices() {
        userChoices.clear();
    }

    @Override
    public List<Movie> getMatchRecommendations(String movie1Id, String movie2Id) {
        // Simple implementation - just return recommendations for the first movie
        List<Movie> recommendations = movieAPIClient.getRecommendations(movie1Id);
        
        // If we don't have enough, add some from the second movie
        if (recommendations.size() < 5) {
            List<Movie> moreRecs = movieAPIClient.getRecommendations(movie2Id);
            
            // Add recommendations from second movie that aren't duplicates
            Set<String> existingIds = recommendations.stream()
                    .map(Movie::getId)
                    .collect(Collectors.toSet());
                    
            for (Movie movie : moreRecs) {
                if (!existingIds.contains(movie.getId())) {
                    recommendations.add(movie);
                    existingIds.add(movie.getId());
                }
                
                if (recommendations.size() >= 10) {
                    break;
                }
            }
        }
        
        // Limit to 10 recommendations
        if (recommendations.size() > 10) {
            recommendations = recommendations.subList(0, 10);
        }
        
        return recommendations;
    }
}