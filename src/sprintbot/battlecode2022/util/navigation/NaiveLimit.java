package sprintbot.battlecode2022.util.navigation;

import sprintbot.battlecode2022.util.Navigator;
import battlecode.common.*;

public class NaiveLimit extends Navigator {

    private final int rubble_limit;

    public NaiveLimit (RobotController controller, int rubble) {
        super(controller);
        rubble_limit = rubble;
    }

    // Naive direct movement
    @Override
    public MoveResult move(MapLocation target_location) throws GameActionException {
        if (target_location == null) return MoveResult.FAIL;
        if (controller.canSenseLocation(target_location) && controller.senseRubble(target_location) >= rubble_limit) return MoveResult.FAIL;
        if (!controller.onTheMap(target_location)) return MoveResult.IMPOSSIBLE;
        if (controller.getLocation().equals(target_location)) return MoveResult.REACHED;
        return move(controller.getLocation()
                .directionTo(target_location));
    }

    public MoveResult move(Direction direction) throws GameActionException {
        if (controller.canSenseLocation(controller.getLocation().add(direction)) && controller.senseRubble(controller.getLocation().add(direction)) >= rubble_limit) return MoveResult.FAIL;
        if (direction != null && controller.canMove(direction)) {
            controller.move(direction);
            return MoveResult.SUCCESS;
        }
        return MoveResult.FAIL;
    }

}
