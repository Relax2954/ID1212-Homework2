/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.model;

/**
 *
 * @author Relax2954
 */
public class Scoring {

    int score;

    public int scoreincrement(int mysco){
    score=score+mysco;
    return score;
    }
        
    public Scoring() {
        score=0;
    }
}
