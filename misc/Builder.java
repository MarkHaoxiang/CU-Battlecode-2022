package sprintbot.battlecode2022;

import battlecode.common.*;
import sprintbot.RunnableBot;
import sprintbot.battlecode2022.util.Cache;
import sprintbot.battlecode2022.util.Communicator;

import java.util.Random;

public class Builder extends RunnableBot {

    /**
     *  Some Builders serve as scouts.
     *  Scouts move along the direction given by Archon and change to a random direction when needed.
     *  Scouts report messages to Communicator.
     */

    private final boolean is_scout;
    private int direction_id;

    private static Team OPPONENT_TEAM;

    private static Random rng = new Random();

    private static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    // New grids in vision after a movement; 13 for diagonal, 9 for horizontal or vertical
    private static final int num_of_new_grids_max = 13;

    // relative to the new location after movement
    private static final int[][] new_grids_dx = {
            {-4, -3, -2, -1, 0, 1, 2, 3, 4, -10, -10, -10, -10},
            {-2, -1, 0, 1, 2, 2, 3, 3, 4, 4, 4, 4, 4},
            {2, 3, 4, 4, 4, 4, 4, 3, 2, -10, -10, -10, -10},
            {4, 4, 4, 4, 4, 3, 2, 1, 0, -1, -2},
            {-4, -3, -2, -1, 0, 1, 2, 3, 4, -10, -10, -10, -10},
            {-4, -4, -4, -4, -4, -3, -2, -1, 0, 1, 2},
            {-2, -3, -4, -4, -4, -4, -4, -3, -2, -10, -10, -10, -10},
            {2, 1, 0, -1, -2, -2, -3, -3, -4, -4, -4, -4, -4}
    };
    private static final int[][] new_grids_dy = {
            {2, 3, 4, 4, 4, 4, 4, 3, 2, -10, -10, -10, -10},
            {4, 4, 4, 4, 4, 3, 3, 2, 2, 1, 0, -1, -2},
            {4, 3, 2, 1, 0, -1, -2, -3, -4, -10, -10, -10, -10},
            {2, 1, 0, -1, -2, -3, -4, -4, -4, -4, -4},
            {-2, -3, -4, -4, -4, -4, -4, -3, -2, -10, -10, -10, -10},
            {2, 1, 0, -1, -2, -3, -4, -4, -4, -4, -4},
            {4, 3, 2, 1, 0, -1, -2, -3, -4, -10, -10, -10, -10},
            {4, 4, 4, 4, 4, 3, 3, 2, 2, 1, 0, -1, -2}
    };


    public Builder(RobotController rc, boolean is_scout, int direction_id) throws GameActionException {
        super(rc);
        this.is_scout = is_scout;
        this.direction_id = direction_id;
        init();
    }

    @Override
    public void init() throws GameActionException {
        super.init();
        OPPONENT_TEAM = controller.getTeam().opponent();
    }

    /**
     * Finds new valid MapLocations in vision range after movement
     * @return new valid MapLocations
     */
    private MapLocation[] newGrids() throws GameActionException {
        MapLocation[] new_grids = new MapLocation[num_of_new_grids_max];
        int cnt = 0;
        for (int i = 0; i < num_of_new_grids_max; i++) {
            if (new_grids_dx[direction_id][i] == -10) {
                break;
            }
            MapLocation grid = controller.getLocation().translate(new_grids_dx[direction_id][i], new_grids_dy[direction_id][i]);
            if (controller.onTheMap(grid)) {
                new_grids[cnt++] = grid;
            }
        }
        if (cnt < num_of_new_grids_max) {
            new_grids[cnt] = new MapLocation(-1, -1);
        }
        return new_grids;
    }

    @Override
    public void turn() throws GameActionException {
        Cache.update();
        if (is_scout) {
            if (!controller.isMovementReady()) {
                return;
            }

            while (!controller.canMove(directions[direction_id])) {
                direction_id = rng.nextInt(directions.length);
            }
            controller.move(directions[direction_id]);

            MapLocation[] new_grids = newGrids();

            // Compress location first before pushing into queue to avoid repetition

            // check Lead & Gold
            int previous_compressed_location = -1;
            for (MapLocation grid : new_grids) {
                if (grid.x == -1) {
                    break;
                }
                int lead = controller.senseLead(grid);
                int gold = controller.senseGold(grid);
                if (lead > 0 || gold > 0) {
                    int compressed_location = Communicator.compressLocation(grid);
                    if (compressed_location != previous_compressed_location) {
                        Communicator.push(Communicator.Event.METAL, compressed_location);
                        previous_compressed_location = compressed_location;
                    }
                }
            }

            // check opponent Archon
            previous_compressed_location = -1;
            for (MapLocation grid : new_grids) {
                if (grid.x == -1) {
                    break;
                }
                if(controller.canSenseRobotAtLocation(grid)) {
                    RobotInfo robot = controller.senseRobotAtLocation(grid);
                    if (robot.team == OPPONENT_TEAM && robot.type == RobotType.ARCHON) {
                        int compressed_location = Communicator.compressLocation(grid);
                        if (compressed_location != previous_compressed_location) {
                            Communicator.push(Communicator.Event.ARCHON, compressed_location);
                            previous_compressed_location = compressed_location;
                        }
                    }
                }
            }

            // check opponent Soldier & Sage
            previous_compressed_location = -1;
            for (MapLocation grid : new_grids) {
                if (grid.x == -1) {
                    break;
                }
                if(controller.canSenseRobotAtLocation(grid)) {
                    RobotInfo robot = controller.senseRobotAtLocation(grid);
                    if (robot.team == OPPONENT_TEAM && (robot.type == RobotType.SOLDIER || robot.type == RobotType.SAGE)) {
                        int compressed_location = Communicator.compressLocation(grid);
                        if (compressed_location != previous_compressed_location) {
                            Communicator.push(Communicator.Event.SOLDIER, compressed_location);
                            previous_compressed_location = compressed_location;
                        }
                    }
                }
            }

            // change direction after seeing Soldier & Sage
            if (previous_compressed_location != -1) {
                direction_id = rng.nextInt(directions.length);
            }
        }
    }
}
