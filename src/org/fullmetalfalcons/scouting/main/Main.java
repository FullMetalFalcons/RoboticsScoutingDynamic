package org.fullmetalfalcons.scouting.main;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.swing.*;

import org.fullmetalfalcons.scouting.elements.Element;
import org.fullmetalfalcons.scouting.equations.Equation;
import org.fullmetalfalcons.scouting.exceptions.ElementParseException;
import org.fullmetalfalcons.scouting.exceptions.EquationParseException;
import org.fullmetalfalcons.scouting.fileio.Reader;
import org.fullmetalfalcons.scouting.fileio.Writer;
import org.fullmetalfalcons.scouting.sql.SqlWriter;
import org.fullmetalfalcons.scouting.teams.Team;

import com.dd.plist.NSDictionary;

/**
 * Team 4557 FullMetalFalcons Scouting Program - Conversion to Excel
 *
 * The program works with the same Config file as the phone app, allowing it to be updated each year with
 * minimal effort.
 *
 * The program then locates plist files generated by the phone app, reads the data inside and exports it to
 * a custom generated Excel Worksheet.
 *
 * This program is distributed under the MIT License, found in LICENSE.md, TL;DR Do whatever, but you can't sell it
 * This program uses the Apache POI, dd-plist, and expr libraries to function, each with their own licenses.
 *
 * @author Dan Herlihy
 * Created by Dan on 1/11/2016.
 */
public class Main {

    private static final ArrayList<Element> ELEMENTS = new ArrayList<>();
    private static final ArrayList<Team> TEAMS = new ArrayList<>();
    private static final ArrayList<Equation> EQUATIONS = new ArrayList<>();
    private static HashMap<Integer,String> TEAM_NAMES;
    //Console spam
    private static final boolean DEBUG = false;

    //1st argument is location of config file
    //2nd argument is location of plist folder
    //3rd argument is location to export results
    public static void main(String args[]){
        try {

            //Crash Test Dummy \/ \/ \/
            //String a = args[20];

            //Program needs to be told where to look for the plist files and config file
            if(args.length<2) {
                sendError("You have not provided a location for plists or config file", true);
            }

            if (!args[0].equalsIgnoreCase("remote")) {
                String plistsLocation = "";
                String configLocation = "";
                String excelLocation = "";
                String SqlLocation = "";
                boolean writeExcel = true;

                switch (args.length){
                    default:

                    case 5:
                        excelLocation = args[4];
        //                    sendError("Argument 4: " + excelLocation,false);
                    case 4:

                        writeExcel = Boolean.parseBoolean(args[3]);
        //                    sendError("Argument 3: " + writeExcel,false);
                    case 3:
                        SqlLocation = args[2];
                    case 2:
                        configLocation = args[0];
                        plistsLocation = args[1];
        //                    sendError("Argument 2: " + plistsLocation,false);

        //                    sendError("Argument 1: " + configLocation,false);

                    break;
                }
                log("Program Starting");
                log("Starting to load configuration");
                //Populates ELEMENTS ArrayList from configuration file
                Reader.loadConfig(configLocation);

                if(ELEMENTS.size()==0){
                    sendError("No Elements found in config file",true);
                }

                log(ELEMENTS.size() + " elements loaded");

                log("Starting to load plists");

                //Populates TEAMS ArrayList from plist files, passes location of plists
                Reader.loadPlists(plistsLocation);

                log(TEAMS.size() + " teams loaded");

                TEAM_NAMES = Reader.loadTeamNames();

                SqlWriter.write(SqlLocation);

                if (writeExcel){
                    log("Starting to write file");

                    //Writes data to Excel spreadsheet
                    Writer.write(excelLocation);
                    //Asks user if they would like to open the Excel workbook
                    exitDialogue();
                }
            } else {
                try {
                    String configLocation = args[1];
                    String plistLocation = args[2];
                    String sqlLocation = args[3];
                    String teamNum = args[4];
                    String password = args[5];

                    log("Program Starting");
                    log("Starting to load configuration");
                    //Populates ELEMENTS ArrayList from configuration file
                    Reader.loadConfig(configLocation);

                    if(ELEMENTS.size()==0){
                        sendError("No Elements found in config file",true);
                    }

                    log(ELEMENTS.size() + " elements loaded");

                    log("Starting to load plists");

                    //Populates TEAMS ArrayList from plist files, passes location of plists
                    Reader.loadPlists(plistLocation);

                    log(TEAMS.size() + " teams loaded");
                    SqlWriter.writeRemote(sqlLocation, teamNum,password);
                } catch (ArrayIndexOutOfBoundsException e){
                    sendError("Not enough arguments: config plists local-database team-num password",true,e);
                }


            }


            log("Exiting program");

            System.exit(0);
        } catch(Exception e){
            e.printStackTrace();
            //If something goes horribly wrong and the exception isn't caught somewhere else in the code

            try (InputStream is = Main.class.getResourceAsStream("/org/fullmetalfalcons/scouting/resources/errors.txt");
                 InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader br = new BufferedReader(isr)){

                //This try/catch statement loads an entire text file into memory just so it can randomly select a funny error message
                //Yes, my ego is this enormous
                ArrayList<String> lines = new ArrayList<>();
                String line;
                while ((line=br.readLine())!=null){
                    if(lines.hashCode()!=797836471){
                        lines.add(line);
                    }
                }

                Random rand = new Random();
                sendError(lines.get(rand.nextInt(lines.size())) + ": " + e.toString(),true,e);

            } catch (IOException e1) {
                //If my ego for some strange reason gets too big, this is where we admit defeat
                sendError("We tried to be funny and we failed. Anyways, the program crashed: " + e.toString(),true,e1);
            }

        }



    }

    /**
     * When the program exits, it asks the user whether or not they would like to open results.xlsx
     *
     * @throws IOException
     */
    private static void exitDialogue() throws IOException {

        //Open a Dialogue box with the options "Yes" and "No"
        int result = JOptionPane.showConfirmDialog(null, "Results have been saved to \""+ Writer.getResultsFile().getAbsolutePath()+ "\" Would you like to open the workbook now?",
                "Done!", JOptionPane.YES_NO_OPTION);

        //If the user says yes, open results.xlsx
        if (result==JOptionPane.YES_OPTION){
            try {
                Desktop.getDesktop().open(Writer.getResultsFile());
            }catch (IllegalArgumentException e){
                sendError("Congratulations! You managed damage/lose the results file in the time since it was made!",true,e);
            }
        }
    }

    /**
     * Creates an Element from a line provided from the config file
     * Adds the Element to the ELEMENTS ArrayList
     *
     * @param line The line from the config file
     */
    public static void addElement(String line){

        try {
            //If something is wrong with "line" the new element will thrown an ElementParseException
            Element e = new Element(line);
            debug("Element of type " + e.getType().toString() + " created");
            ELEMENTS.add(e);
        } catch (ElementParseException e) {
            sendError("Config error: " + e.getMessage(),false,e);
        }
    }

    /**
     * Creates a Team Object which essentially just holds an NSDictionary
     * Adds it to TEAMS ArrayList
     *
     * @param dictionary Holds the key/value pairs for the team
     * @param fileName Name of the team's file
     */
    public static void addTeam(NSDictionary dictionary, String fileName){
        Team t = new Team(dictionary, fileName);
        debug("Team " + t.getValue(Team.NUMBER_KEY) + " loaded");
        TEAMS.add(t);
    }

    /**
     * Creates an Equation from a String and adds it to the EQUATIONS ArrayList
     *
     * @param line Line from the config file to pass to the equation
     */
    public static void addEquation(String line){
        try {
            Equation e = new Equation(line);
            debug("Equation " + line + " added");
            EQUATIONS.add(e);
        } catch (EquationParseException e){
            sendError(e.getMessage(),false,e);
        }
    }

    /**
     * Simple getter for ELEMENTS ArrayList
     *
     * @return All Elements loaded by the program
     */
    public static ArrayList<Element> getElements(){
        return ELEMENTS;
    }

    /**
     * Simple getter for TEAMS ArrayList
     *
     * @return All Teams loaded by the program
     */
    public static ArrayList<Team> getTeams() {
        return TEAMS;
    }

    /**
     * Sends an error message to the user with a JOptionPane
     *
     * @param message error message to send
     * @param isCatastrophicError program will close after acknowledging message
     * @param e Exception to print stack trace
     */
    public static void sendError(String message, boolean isCatastrophicError, Exception e){
        Main.log("[ERROR]:"+message);
        if (e!=null){
            e.printStackTrace();
        }
        try {
            JTextArea msg = new JTextArea(message);
            msg.setLineWrap(true);
            msg.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(msg);
            msg.setSize(new Dimension(600,200));

            //Open a Dialogue box with only "OK" as an option
            int i = JOptionPane.showConfirmDialog(null, scrollPane,
                    "You done messed up", JOptionPane.DEFAULT_OPTION,JOptionPane.ERROR_MESSAGE);

            //If the error is catastrophic, close the program
            if(isCatastrophicError){
                System.exit(-1);
            //Or, if the user clicks the "X" button, close the program
            } else if (i==JOptionPane.CLOSED_OPTION){
                System.exit(1);
            }

        } catch (Exception e1){
            //Just in case
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred while displaying an error",
                    "Yo Dawg!", JOptionPane.ERROR_MESSAGE);

        }

    }

    public static void sendError(String message, boolean isCatastrophicError){
        sendError(message,isCatastrophicError,null);
    }

    /**
     * Sends a message to console, but only if debug is turned on
     *
     * @param message Message to be sent to console
     */
    public static void debug(String message){
        if (DEBUG){
            System.out.println(message);
        }
    }

    /**
     * Used instead of System.out in case of a need to do something else
     *
     * @param message Message to be sent to console
     */
    public static void log(String message){
        System.out.println(message);
    }

    /**
     * Simple getter for EQUATIONS ArrayList
     *
     * @return All Equations loaded by the program
     */
    public static ArrayList<Equation> getEquations() {
        return EQUATIONS;
    }

    public static String getTeamName(int teamNum) {
        try {
            return TEAM_NAMES.get(teamNum);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return "MISSING NAME";
    }
}
