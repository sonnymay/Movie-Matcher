package com.yourname.moviematcher.model;

import java.util.HashSet;
import java.util.Set;

public class Movie {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String platforms; // Changed from Set<String> to String
    private String genre;
    private double rating;
    private Set<String> genres = new HashSet<>();

    public Movie(String id, String title, String description, String imageUrl, String platforms, String genre, double rating) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.platforms = platforms;
        this.genre = genre;
        this.rating = rating;
        
        // Add primary genre to genres set
        this.genres.add(genre);
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getPlatforms() { return platforms; }
    public String getGenre() { return genre; }
    public double getRating() { return rating; }
    public Set<String> getGenres() { return genres; }
    
    // Setters
    public void setGenres(Set<String> genres) { 
        this.genres = genres; 
        // If genre is empty, update it with first genre from set
        if (this.genre == null || this.genre.equals("Unknown") || this.genre.equals("General")) {
            if (!genres.isEmpty()) {
                this.genre = genres.iterator().next();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return id.equals(movie.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", rating=" + rating +
                '}';
    }
}