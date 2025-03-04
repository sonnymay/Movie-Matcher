package com.yourname.moviematcher.service;

import java.util.List;
import com.yourname.moviematcher.model.Movie;

public interface MovieService {
    
    /**
     * Gets a list of popular movies
     */
    List<Movie> getPopularMovies();
    
    /**
     * Gets a list of popular TV shows
     */
    List<Movie> getPopularTvShows();
    
    /**
     * Gets all content (movies and TV shows)
     */
    List<Movie> getAllContent();
    
    /**
     * Refreshes the movie list from the API
     */
    void refreshMovies();
    
    /**
     * Gets movie details by ID
     */
    Movie getMovieDetails(String id);
    
    /**
     * Gets the next two choices for the user to pick from
     */
    List<Movie> getNextChoices(String platform);
    
    /**
     * Records the user's choice
     */
    void recordChoice(String chosenId);
    
    /**
     * Gets the final recommendation based on user choices
     */
    Movie getFinalRecommendation();
    
    /**
     * Resets user choices
     */
    void resetChoices();
    
    /**
     * Gets recommendations based on two movie selections
     */
    List<Movie> getMatchRecommendations(String movie1Id, String movie2Id);
}