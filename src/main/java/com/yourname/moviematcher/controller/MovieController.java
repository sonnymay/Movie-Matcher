package com.yourname.moviematcher.controller;

import java.util.Arrays;
import java.util.List;

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
                       Model model) {
        
        try {
            if (round >= 10) {
                Movie finalRecommendation = movieService.getFinalRecommendation();
                model.addAttribute("movie", finalRecommendation);
                return "recommendation";
            }
            
            List<Movie> choices = movieService.getNextChoices(platform);
            
            if (choices.size() < 2) {
                model.addAttribute("errorMessage", "Not enough content available. Please try a different platform.");
                return "error";
            }
            
            model.addAttribute("leftMovie", choices.get(0));
            model.addAttribute("rightMovie", choices.get(1));
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
                      @RequestParam int round) {
        try {
            // We don't record any choice, just move to the next pair
            return "redirect:/match?platform=" + platform + "&round=" + round;
        } catch (Exception e) {
            System.err.println("Error in skip action: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/error";
        }
    }
    
    @GetMapping("/continue")
    public String continueMatching() {
        movieService.resetChoices();
        return "redirect:/";
    }
    
    @GetMapping("/error")
    public String error() {
        return "error";
    }
}