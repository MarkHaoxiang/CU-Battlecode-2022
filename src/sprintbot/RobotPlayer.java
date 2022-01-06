package sprintbot;

import battlecode.common.*;
import sprintbot.battlecode2022.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {
    public static RobotController controller;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.controller = rc;

        RunnableBot bot;
        switch (controller.getType()) {
            case ARCHON:
                bot = new Archon(controller);
                break;
            case BUILDER:
                bot = new Builder(controller);
                break;
            case LABORATORY:
                bot = new Laboratory(controller);
                break;
            case MINER:
                bot = new Miner(controller);
                break;
            case SAGE:
                bot = new Sage(controller);
                break;
            case SOLDIER:
                bot = new Soldier(controller);
                break;
            case WATCHTOWER:
                bot = new Watchtower(controller);
                break;
            default:
                throw new IllegalStateException("NOT A VALID BOT");
        }

        bot.turn();
    }
}