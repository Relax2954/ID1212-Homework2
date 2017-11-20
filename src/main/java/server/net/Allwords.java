/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.net;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/**
 *
 * @author Relax2954
 */
public class Allwords {

    File allwords = new File("words.txt");
    ArrayList<String> names = new ArrayList<String>();

    public Allwords() throws FileNotFoundException { //this is for allwords- i want the list of words to be available to everywhere

        names = new ArrayList<String>();
        Scanner in = new Scanner(allwords);
        while (in.hasNextLine()) {
            names.add(in.nextLine());
        }
        Collections.sort(names); //? ovo samo sortira, nema potrebe za ovim uopste?
    }
    
    public ArrayList<String> getList() {
       return names;
   }

}
