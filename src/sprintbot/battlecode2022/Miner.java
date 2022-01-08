package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.*;

import java.util.*;


public class Miner extends RunnableBot {


    // Temporary for testing
    private MapLocation move_target;

    public Miner(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void init() throws GameActionException {
        super.init();
        move_target = navigator.randomLocation();
    }

    @Override
    public void turn() throws GameActionException {

        Cache.update();

        // Local variables save bytecode
        RobotController controller = getRobotController();

        // For testing, delete if actual strategy ready

        // Try to mine on squares around us.
        MapLocation me = controller.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (controller.canMineGold(mineLocation)) {
                    controller.mineGold(mineLocation);
                }
                while (controller.canMineLead(mineLocation)) {
                    controller.mineLead(mineLocation);
                }
            }
        }

        // Move towards closest lead
        MapLocation[] lead_spots = controller.senseNearbyLocationsWithLead(RobotType.MINER.visionRadiusSquared);

        if (lead_spots.length > 0) {
            Arrays.sort(lead_spots, new Comparator<MapLocation>() {
                @Override
                public int compare(MapLocation mapLocation, MapLocation t1) {
                    return Navigator.travelDistance(controller.getLocation(), mapLocation).
                            compareTo(Navigator.travelDistance(controller.getLocation(), t1));
                }
            });
            move_target = lead_spots[0];
            navigator.move(move_target);
        } else {
            Navigator.MoveResult move_result = navigator.move(move_target);
            // Random movement
            if (move_result == Navigator.MoveResult.IMPOSSIBLE ||
                    move_result == Navigator.MoveResult.REACHED) {
                move_target = navigator.randomLocation();
            }

        }
    }
}
