/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.model;

import java.util.Arrays;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Relax2954
 */
public class GameLogicC {

    public volatile int score; //the total current score;
    public String chosenword;
    public volatile String checkerString;   //SHOULD IT BE VOLATILE, MAYBE ATOMIC?  check private?
    public volatile int remaining = 0;  //remaining shots to take
    private char[] checker; //ovdje stavlja  capword cifru lokacije pogodjenog slova

    public static String chooseWord() throws FileNotFoundException { //this is choosing a word from words.txt
        Allwords myallwords = new Allwords();
        Random rand = new Random();
        ArrayList<String> names = myallwords.getList();
        String randomword = names.get(rand.nextInt(names.size()));
        return randomword;
    }

    public GameLogicC() throws FileNotFoundException { //start game

        chosenword = chooseWord();
        remaining = chosenword.length();
        checker = new char[chosenword.length()];
        Arrays.fill(checker, '_');
        checkerString = String.valueOf(checker);
    }

    public String gamelogic(String theword, String myguess) { //this is where the game logic magic happens
        if (theword == null) {
            return "Please start the game before guessing";
        }
        String capguess = myguess.toLowerCase();
        String capword = theword.toLowerCase();
        char[] capwordarray = capword.toLowerCase().toCharArray();

        if (remaining == 0) {
            score = 0;  //I added this because of the printout of the score which is done in the ClientHandler!
            return "Please start a new game.";
        } else if (capguess.length() != capword.length() && capguess.length() != 1) {
            remaining--;
            checkerString = String.valueOf(checker);
            if (remaining == 0) {
                score--;
                return checkerString + "\nRemaining attempts: " + remaining;
            }
            return checkerString + "\nRemaining attempts: " + remaining;
        } else if (capguess.equals(capword)) {
            score++;
            remaining = 0;
            return capword;
        } else if (!capword.contains(capguess)) {
            remaining--;
            checkerString = String.valueOf(checker);
            if (remaining == 0) {
                score--;
                return checkerString + "\nRemaining attempts: " + remaining;
            }
            return checkerString + "\nRemaining attempts: " + remaining;
        } else {
            for (int i = 0; i < capword.length(); i++) {
                if (capguess.charAt(0) == capwordarray[i]) {
                    checker[i] = capguess.charAt(0);  //u ovom praznom array stavlja guess slova
                }
            }
            checkerString = String.valueOf(checker);
            if (!checkerString.contains("_")) {
                score++;
                remaining = 0;
                return capword;
            }
            return checkerString + "\nRemaining attempts: " + remaining;
        }
    }
}
