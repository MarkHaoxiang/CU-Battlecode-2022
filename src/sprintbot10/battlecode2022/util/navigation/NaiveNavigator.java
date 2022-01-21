package sprintbot10.battlecode2022.util.navigation;

import sprintbot10.battlecode2022.util.Navigator;
import battlecode.common.*;

public class NaiveNavigator extends Navigator {


    public NaiveNavigator(RobotController controller) {
        super(controller);
    }

    // Naive direct movement
    @Override
    public MoveResult move(MapLocation target_location) throws GameActionException {
        if (target_location == null) return MoveResult.FAIL;
        if (!controller.onTheMap(target_location)) return MoveResult.IMPOSSIBLE;
        if (controller.getLocation().equals(target_location)) return MoveResult.REACHED;
        return move(controller.getLocation()
                .directionTo(target_location));
    }

}
