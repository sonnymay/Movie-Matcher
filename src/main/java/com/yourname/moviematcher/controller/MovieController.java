package com.yourname.moviematcher.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yourname.moviematcher.model.Movie;
import com.yourname.moviematcher.service.MovieService;

@Controller
public class MovieController {
    
    @Autowired
    private MovieService movieService;
    
    // Store the previously shown movie IDs to avoid repetition
    private Set<String> recentlyShownIds = new HashSet<>();
    
    @GetMapping("/")
    public String index(Model model) {
        try {
            // Get popular movies and TV shows for display
            List<Movie> popularMovies = movieService.getPopularMovies();
            List<Movie> popularTvShows = movieService.getPopularTvShows();
            
            // Limit to 4 items each for display
            model.addAttribute("popularMovies", popularMovies.subList(0, Math.min(4, popularMovies.size())));
            model.addAttribute("popularTvShows", popularTvShows.subList(0, Math.min(4, popularTvShows.size())));
            
            // Add available platforms
            model.addAttribute("platforms", Arrays.asList("Netflix", "Max", "Prime", "All"));
            
            // Clear recently shown movies when returning to home
            recentlyShownIds.clear();
            
            return "index";
        } catch (Exception e) {
            System.err.println("Error in index page: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "An error occurred loading popular content. Please try again later.");
            return "error";
        }
    }
    
    @GetMapping("/match")
    public String match(@RequestParam(defaultValue = "All") String platform, 
                       @RequestParam(defaultValue = "0") int round,
                       @RequestParam(required = false) Long timestamp,
                       Model model) {
        
        try {
            if (round >= 10) {
                Movie finalRecommendation = movieService.getFinalRecommendation();
                model.addAttribute("movie", finalRecommendation);
                return "recommendation";
            }
            
            // Get multiple choices and filter out recently shown movies
            List<Movie> allChoices = movieService.getNextChoices(platform);
            
            // Filter out recently shown movies (max 10 attempts to prevent infinite loop)
            List<Movie> filteredChoices = allChoices;
            for (int attempt = 0; attempt < 10; attempt++) {
                filteredChoices = allChoices.stream()
                    .filter(movie -> !recentlyShownIds.contains(movie.getId()))
                    .toList();
                
                // If we have at least 2 movies after filtering, we're good
                if (filteredChoices.size() >= 2) {
                    break;
                }
                
                // If not enough choices after filtering, try to get more from service
                allChoices = movieService.getNextChoices(platform);
                
                // If still not enough after 5 attempts, clear the recently shown set
                if (attempt >= 5) {
                    recentlyShownIds.clear();
                }
            }
            
            // If still not enough choices, fall back to unfiltered list
            if (filteredChoices.size() < 2) {
                filteredChoices = allChoices;
            }
            
            if (filteredChoices.size() < 2) {
                model.addAttribute("errorMessage", "Not enough content available. Please try a different platform.");
                return "error";
            }
            
            // Select two movies for display
            Movie leftMovie = filteredChoices.get(0);
            Movie rightMovie = filteredChoices.get(1);
            
            // Add to recently shown set to avoid repetition
            recentlyShownIds.add(leftMovie.getId());
            recentlyShownIds.add(rightMovie.getId());
            
            // Keep set size manageable (max 20 recent movies)
            if (recentlyShownIds.size() > 20) {
                // Convert to list, remove oldest items
                List<String> ids = recentlyShownIds.stream().toList();
                recentlyShownIds = new HashSet<>(ids.subList(ids.size() - 20, ids.size()));
            }
            
            model.addAttribute("leftMovie", leftMovie);
            model.addAttribute("rightMovie", rightMovie);
            model.addAttribute("round", round);
            model.addAttribute("platform", platform);
            
            return "match";
        } catch (Exception e) {
            System.err.println("Error in match page: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "An error occurred during matching. Please try again later.");
            return "error";
        }
    }
    
    @PostMapping("/choose")
    public String choose(@RequestParam String chosenId, 
                        @RequestParam String platform,
                        @RequestParam int round) {
        
        try {
            movieService.recordChoice(chosenId);
            return "redirect:/match?platform=" + platform + "&round=" + (round + 1);
        } catch (Exception e) {
            System.err.println("Error in choose action: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/error";
        }
    }
    
    @GetMapping("/skip")
    public String skip(@RequestParam String platform,
                      @RequestParam int round,
                      @RequestParam(required = false) String leftId,
                      @RequestParam(required = false) String rightId) {
        try {
            // Skip both movies in the service
            if (leftId != null && rightId != null) {
                movieService.skipCurrentPair(leftId, rightId);
            }
            
            // Add a timestamp parameter to prevent caching and ensure new content
            long timestamp = System.currentTimeMillis();
            return "redirect:/match?platform=" + platform + "&round=" + round + "&timestamp=" + timestamp;
        } catch (Exception e) {
            System.err.println("Error in skip action: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/error";
        }
    }
    
    @GetMapping("/continue")
    public String continueMatching() {
        movieService.resetChoices();
        recentlyShownIds.clear(); // Clear recently shown movies
        return "redirect:/";
    }
    
    @GetMapping("/error")
    public String error(Model model) {
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        return "error";
    }
}