package com.yourname.moviematcher.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.yourname.moviematcher.model.Movie;

/**
 * Client for accessing The Movie Database (TMDB) API
 */
@Component
public class MovieAPIClient {

    @Value("${tmdb.api.key}")
    private String apiKey;
    
    private final HttpClient httpClient;
    private final String BASE_URL = "https://api.themoviedb.org/3";
    private final Map<Integer, String> genreMap;
    
    public MovieAPIClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
                
        // Initialize genre map
        genreMap = new HashMap<>();
        initializeGenreMap();
    }
    
    /**
     * Initialize genre ID to name mapping
     */
    private void initializeGenreMap() {
        // Movie genres
        genreMap.put(28, "Action");
        genreMap.put(12, "Adventure");
        genreMap.put(16, "Animation");
        genreMap.put(35, "Comedy");
        genreMap.put(80, "Crime");
        genreMap.put(99, "Documentary");
        genreMap.put(18, "Drama");
        genreMap.put(10751, "Family");
        genreMap.put(14, "Fantasy");
        genreMap.put(36, "History");
        genreMap.put(27, "Horror");
        genreMap.put(10402, "Music");
        genreMap.put(9648, "Mystery");
        genreMap.put(10749, "Romance");
        genreMap.put(878, "Science Fiction");
        genreMap.put(10770, "TV Movie");
        genreMap.put(53, "Thriller");
        genreMap.put(10752, "War");
        genreMap.put(37, "Western");
        
        // TV show genres
        genreMap.put(10759, "Action & Adventure");
        genreMap.put(10762, "Kids");
        genreMap.put(10763, "News");
        genreMap.put(10764, "Reality");
        genreMap.put(10765, "Sci-Fi & Fantasy");
        genreMap.put(10766, "Soap");
        genreMap.put(10767, "Talk");
        genreMap.put(10768, "War & Politics");
    }
    
    /**
     * Gets popular movies from TMDB API
     */
    @Cacheable("popularMovies")
    public List<Movie> getPopularMovies() {
        try {
            String url = BASE_URL + "/movie/popular?api_key=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                    
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                System.err.println("API request failed: " + response.statusCode());
                return new ArrayList<>();
            }
            
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray results = jsonResponse.getJSONArray("results");
            
            return parseMovieList(results);
        } catch (Exception e) {
            System.err.println("Error fetching popular movies: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets popular TV shows from TMDB API
     */
    @Cacheable("popularTvShows")
    public List<Movie> getPopularTvShows() {
        try {
            String url = BASE_URL + "/tv/popular?api_key=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                    
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return new ArrayList<>();
            }
            
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray results = jsonResponse.getJSONArray("results");
            
            return parseTvShowList(results);
        } catch (Exception e) {
            System.err.println("Error fetching popular TV shows: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets movie details by ID
     */
    @Cacheable("movieDetails")
    public Movie getMovieDetails(String id) {
        try {
            // Try to parse as integer to determine if it's a movie or TV show
            int idNum = Integer.parseInt(id);
            
            // First try as movie
            String url = BASE_URL + "/movie/" + id + "?api_key=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                    
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject details = new JSONObject(response.body());
                return parseMovieDetails(details);
            }
            
            // If movie fails, try as TV show
            url = BASE_URL + "/tv/" + id + "?api_key=" + apiKey;
            
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                    
            response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                    
            if (response.statusCode() == 200) {
                JSONObject details = new JSONObject(response.body());
                return parseTvDetails(details);
            }
            
            return createDefaultMovie(id);
        } catch (Exception e) {
            System.err.println("Error fetching movie details: " + e.getMessage());
            return createDefaultMovie(id);
        }
    }
    
    /**
     * Gets similar movies/shows to the given one
     */
    @Cacheable("recommendations")
    public List<Movie> getRecommendations(String id) {
        try {
            // First try as movie
            String url = BASE_URL + "/movie/" + id + "/recommendations?api_key=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                    
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray results = jsonResponse.getJSONArray("results");
                return parseMovieList(results);
            }
            
            // If movie fails, try as TV show
            url = BASE_URL + "/tv/" + id + "/recommendations?api_key=" + apiKey;
            
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                    
            response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                    
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray results = jsonResponse.getJSONArray("results");
                return parseTvShowList(results);
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error fetching recommendations: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets content filtered by platform
     */
    public List<Movie> getContentByPlatform(String platform) {
        // For simplicity in this first version, just return all content
        List<Movie> allContent = new ArrayList<>();
        allContent.addAll(getPopularMovies());
        allContent.addAll(getPopularTvShows());
        
        return allContent;
    }
    
    /**
     * Parses movie list from JSON response
     */
    private List<Movie> parseMovieList(JSONArray results) {
        List<Movie> movies = new ArrayList<>();
        
        for (int i = 0; i < results.length(); i++) {
            JSONObject movieJson = results.getJSONObject(i);
            
            // Convert numeric ID to string
            String id = String.valueOf(movieJson.getInt("id"));
            
            String title = movieJson.getString("title");
            
            String overview = movieJson.has("overview") ? movieJson.getString("overview") : "";
            if (overview.isEmpty()) {
                overview = "A movie in the " + getGenreNames(movieJson) + " genre.";
            }
            
            // Get just first sentence or limit to 150 chars
            if (overview.length() > 150) {
                int endIndex = overview.indexOf(". ");
                if (endIndex > 0 && endIndex < 150) {
                    overview = overview.substring(0, endIndex + 1);
                } else {
                    overview = overview.substring(0, 147) + "...";
                }
            }
            
            // Build image URL
            String posterPath = movieJson.optString("poster_path", null);
            String imageUrl = posterPath != null ? 
                    "https://image.tmdb.org/t/p/w500" + posterPath : 
                    "https://via.placeholder.com/500x750?text=No+Image";
            
            // Get a default genre
            String genre = getGenreNames(movieJson);
            
            // Get rating
            double rating = movieJson.has("vote_average") ? 
                    movieJson.getDouble("vote_average") : 0;
            
            // For now, use placeholder provider
            String platforms = "Unknown";
            
            // Get genres
            Set<String> genres = new HashSet<>();
            if (movieJson.has("genre_ids")) {
                JSONArray genreIds = movieJson.getJSONArray("genre_ids");
                for (int j = 0; j < genreIds.length(); j++) {
                    int genreId = genreIds.getInt(j);
                    genres.add(getGenreName(genreId));
                }
            }
            
            Movie movie = new Movie(id, title, overview, imageUrl, platforms, genre, rating);
            movie.setGenres(genres);
            movies.add(movie);
        }
        
        return movies;
    }
    
    /**
     * Parses TV show list from JSON response
     */
    private List<Movie> parseTvShowList(JSONArray results) {
        List<Movie> shows = new ArrayList<>();
        
        for (int i = 0; i < results.length(); i++) {
            JSONObject showJson = results.getJSONObject(i);
            
            // Convert numeric ID to string
            String id = String.valueOf(showJson.getInt("id"));
            
            String title = showJson.getString("name");
            
            String overview = showJson.has("overview") ? showJson.getString("overview") : "";
            if (overview.isEmpty()) {
                overview = "A TV show in the " + getGenreNames(showJson) + " genre.";
            }
            
            // Get just first sentence or limit to 150 chars
            if (overview.length() > 150) {
                int endIndex = overview.indexOf(". ");
                if (endIndex > 0 && endIndex < 150) {
                    overview = overview.substring(0, endIndex + 1);
                } else {
                    overview = overview.substring(0, 147) + "...";
                }
            }
            
            // Build image URL
            String posterPath = showJson.optString("poster_path", null);
            String imageUrl = posterPath != null ? 
                    "https://image.tmdb.org/t/p/w500" + posterPath : 
                    "https://via.placeholder.com/500x750?text=No+Image";
            
            // Get a default genre
            String genre = getGenreNames(showJson);
            
            // Get rating
            double rating = showJson.has("vote_average") ? 
                    showJson.getDouble("vote_average") : 0;
            
            // For now, use placeholder provider
            String platforms = "Unknown";
            
            // Get genres
            Set<String> genres = new HashSet<>();
            if (showJson.has("genre_ids")) {
                JSONArray genreIds = showJson.getJSONArray("genre_ids");
                for (int j = 0; j < genreIds.length(); j++) {
                    int genreId = genreIds.getInt(j);
                    genres.add(getGenreName(genreId));
                }
            }
            
            Movie show = new Movie(id, title, overview, imageUrl, platforms, genre, rating);
            show.setGenres(genres);
            shows.add(show);
        }
        
        return shows;
    }
    
    /**
     * Parse movie details from JSON response
     */
    private Movie parseMovieDetails(JSONObject details) {
        String id = String.valueOf(details.getInt("id"));
        String title = details.getString("title");
        
        String overview = details.has("overview") ? details.getString("overview") : "";
        if (overview.isEmpty()) {
            overview = "No description available.";
        }
        
        // Build image URL
        String posterPath = details.optString("poster_path", null);
        String imageUrl = posterPath != null ? 
                "https://image.tmdb.org/t/p/w500" + posterPath : 
                "https://via.placeholder.com/500x750?text=No+Image";
        
        // Get genres
        Set<String> genres = new HashSet<>();
        String primaryGenre = "Unknown";
        
        if (details.has("genres")) {
            JSONArray genreArray = details.getJSONArray("genres");
            for (int i = 0; i < genreArray.length(); i++) {
                JSONObject genre = genreArray.getJSONObject(i);
                String genreName = genre.getString("name");
                genres.add(genreName);
                
                if (i == 0) {
                    primaryGenre = genreName;
                }
            }
        }
        
        // Get rating
        double rating = details.has("vote_average") ? 
                details.getDouble("vote_average") : 0;
        
        // For now, use placeholder provider
        String platforms = "Unknown";
        
        Movie movie = new Movie(id, title, overview, imageUrl, platforms, primaryGenre, rating);
        movie.setGenres(genres);
        
        return movie;
    }
    
    /**
     * Parse TV show details from JSON response
     */
    private Movie parseTvDetails(JSONObject details) {
        String id = String.valueOf(details.getInt("id"));
        String title = details.getString("name");
        
        String overview = details.has("overview") ? details.getString("overview") : "";
        if (overview.isEmpty()) {
            overview = "No description available.";
        }
        
        // Build image URL
        String posterPath = details.optString("poster_path", null);
        String imageUrl = posterPath != null ? 
                "https://image.tmdb.org/t/p/w500" + posterPath : 
                "https://via.placeholder.com/500x750?text=No+Image";
        
        // Get genres
        Set<String> genres = new HashSet<>();
        String primaryGenre = "Unknown";
        
        if (details.has("genres")) {
            JSONArray genreArray = details.getJSONArray("genres");
            for (int i = 0; i < genreArray.length(); i++) {
                JSONObject genre = genreArray.getJSONObject(i);
                String genreName = genre.getString("name");
                genres.add(genreName);
                
                if (i == 0) {
                    primaryGenre = genreName;
                }
            }
        }
        
        // Get rating
        double rating = details.has("vote_average") ? 
                details.getDouble("vote_average") : 0;
        
        // For now, use placeholder provider
        String platforms = "Unknown";
        
        Movie show = new Movie(id, title, overview, imageUrl, platforms, primaryGenre, rating);
        show.setGenres(genres);
        
        return show;
    }
    
    /**
     * Create a default movie with minimal info
     */
    private Movie createDefaultMovie(String id) {
        return new Movie(
            id, 
            "Movie " + id, 
            "No description available.", 
            "https://via.placeholder.com/500x750?text=No+Image", 
            "Unknown", 
            "Unknown", 
            0
        );
    }
    
    /**
     * Gets genre names from genre IDs
     */
    private String getGenreNames(JSONObject item) {
        if (!item.has("genre_ids") || item.getJSONArray("genre_ids").length() == 0) {
            return "General";
        }
        
        JSONArray genreIds = item.getJSONArray("genre_ids");
        int genreId = genreIds.getInt(0);
        
        return getGenreName(genreId);
    }
    
    /**
     * Get genre name from ID
     */
    private String getGenreName(int genreId) {
        return genreMap.getOrDefault(genreId, "General");
    }
}