#include <cstdio>
#include <string>
using namespace std;
int vision;
void printHead(){printf(R"(package sprintbot.battlecode2022.util.navigation;

import battlecode.common.*;
import static battlecode.common.Direction.*;
import sprintbot.battlecode2022.util.Navigator;

public class DPNavigator extends Navigator
{
	private static final int INF_MARBLE = 1000000;
	private RobotController rc;
	private final int UPPER_BOUND_TURN_PER_GRID;
	private final int MOVE_COOLDOWN;
	private final int X_BOUND;
	private final int Y_BOUND;
	private final int[] MARBLE_COST_LOOKUP;
	
	public DPNavigator(RobotController controller)
	{
		super(controller);
		rc = controller;
		X_BOUND = rc.getMapWidth() - 1;    // [0, X_BOUND]
		Y_BOUND = rc.getMapHeight() - 1;
		MOVE_COOLDOWN = rc.getType().movementCooldown;
		UPPER_BOUND_TURN_PER_GRID = MOVE_COOLDOWN * 110;
		MARBLE_COST_LOOKUP = new int[]{(int) (1.0 * MOVE_COOLDOWN) * 10,
				(int) (1.1 * MOVE_COOLDOWN) * 10,
				(int) (1.2 * MOVE_COOLDOWN) * 10,
				(int) (1.3 * MOVE_COOLDOWN) * 10,
				(int) (1.4 * MOVE_COOLDOWN) * 10,
				(int) (1.5 * MOVE_COOLDOWN) * 10,
				(int) (1.6 * MOVE_COOLDOWN) * 10,
				(int) (1.7 * MOVE_COOLDOWN) * 10,
				(int) (1.8 * MOVE_COOLDOWN) * 10,
				(int) (1.9 * MOVE_COOLDOWN) * 10,
				(int) (2.0 * MOVE_COOLDOWN) * 10,
				(int) (2.1 * MOVE_COOLDOWN) * 10,
				(int) (2.2 * MOVE_COOLDOWN) * 10,
				(int) (2.3 * MOVE_COOLDOWN) * 10,
				(int) (2.4 * MOVE_COOLDOWN) * 10,
				(int) (2.5 * MOVE_COOLDOWN) * 10,
				(int) (2.6 * MOVE_COOLDOWN) * 10,
				(int) (2.7 * MOVE_COOLDOWN) * 10,
				(int) (2.8 * MOVE_COOLDOWN) * 10,
				(int) (2.9 * MOVE_COOLDOWN) * 10,
				(int) (3.0 * MOVE_COOLDOWN) * 10,
				(int) (3.1 * MOVE_COOLDOWN) * 10,
				(int) (3.2 * MOVE_COOLDOWN) * 10,
				(int) (3.3 * MOVE_COOLDOWN) * 10,
				(int) (3.4 * MOVE_COOLDOWN) * 10,
				(int) (3.5 * MOVE_COOLDOWN) * 10,
				(int) (3.6 * MOVE_COOLDOWN) * 10,
				(int) (3.7 * MOVE_COOLDOWN) * 10,
				(int) (3.8 * MOVE_COOLDOWN) * 10,
				(int) (3.9 * MOVE_COOLDOWN) * 10,
				(int) (4.0 * MOVE_COOLDOWN) * 10,
				(int) (4.1 * MOVE_COOLDOWN) * 10,
				(int) (4.2 * MOVE_COOLDOWN) * 10,
				(int) (4.3 * MOVE_COOLDOWN) * 10,
				(int) (4.4 * MOVE_COOLDOWN) * 10,
				(int) (4.5 * MOVE_COOLDOWN) * 10,
				(int) (4.6 * MOVE_COOLDOWN) * 10,
				(int) (4.7 * MOVE_COOLDOWN) * 10,
				(int) (4.8 * MOVE_COOLDOWN) * 10,
				(int) (4.9 * MOVE_COOLDOWN) * 10,
				(int) (5.0 * MOVE_COOLDOWN) * 10,
				(int) (5.1 * MOVE_COOLDOWN) * 10,
				(int) (5.2 * MOVE_COOLDOWN) * 10,
				(int) (5.3 * MOVE_COOLDOWN) * 10,
				(int) (5.4 * MOVE_COOLDOWN) * 10,
				(int) (5.5 * MOVE_COOLDOWN) * 10,
				(int) (5.6 * MOVE_COOLDOWN) * 10,
				(int) (5.7 * MOVE_COOLDOWN) * 10,
				(int) (5.8 * MOVE_COOLDOWN) * 10,
				(int) (5.9 * MOVE_COOLDOWN) * 10,
				(int) (6.0 * MOVE_COOLDOWN) * 10,
				(int) (6.1 * MOVE_COOLDOWN) * 10,
				(int) (6.2 * MOVE_COOLDOWN) * 10,
				(int) (6.3 * MOVE_COOLDOWN) * 10,
				(int) (6.4 * MOVE_COOLDOWN) * 10,
				(int) (6.5 * MOVE_COOLDOWN) * 10,
				(int) (6.6 * MOVE_COOLDOWN) * 10,
				(int) (6.7 * MOVE_COOLDOWN) * 10,
				(int) (6.8 * MOVE_COOLDOWN) * 10,
				(int) (6.9 * MOVE_COOLDOWN) * 10,
				(int) (7.0 * MOVE_COOLDOWN) * 10,
				(int) (7.1 * MOVE_COOLDOWN) * 10,
				(int) (7.2 * MOVE_COOLDOWN) * 10,
				(int) (7.3 * MOVE_COOLDOWN) * 10,
				(int) (7.4 * MOVE_COOLDOWN) * 10,
				(int) (7.5 * MOVE_COOLDOWN) * 10,
				(int) (7.6 * MOVE_COOLDOWN) * 10,
				(int) (7.7 * MOVE_COOLDOWN) * 10,
				(int) (7.8 * MOVE_COOLDOWN) * 10,
				(int) (7.9 * MOVE_COOLDOWN) * 10,
				(int) (8.0 * MOVE_COOLDOWN) * 10,
				(int) (8.1 * MOVE_COOLDOWN) * 10,
				(int) (8.2 * MOVE_COOLDOWN) * 10,
				(int) (8.3 * MOVE_COOLDOWN) * 10,
				(int) (8.4 * MOVE_COOLDOWN) * 10,
				(int) (8.5 * MOVE_COOLDOWN) * 10,
				(int) (8.6 * MOVE_COOLDOWN) * 10,
				(int) (8.7 * MOVE_COOLDOWN) * 10,
				(int) (8.8 * MOVE_COOLDOWN) * 10,
				(int) (8.9 * MOVE_COOLDOWN) * 10,
				(int) (9.0 * MOVE_COOLDOWN) * 10,
				(int) (9.1 * MOVE_COOLDOWN) * 10,
				(int) (9.2 * MOVE_COOLDOWN) * 10,
				(int) (9.3 * MOVE_COOLDOWN) * 10,
				(int) (9.4 * MOVE_COOLDOWN) * 10,
				(int) (9.5 * MOVE_COOLDOWN) * 10,
				(int) (9.6 * MOVE_COOLDOWN) * 10,
				(int) (9.7 * MOVE_COOLDOWN) * 10,
				(int) (9.8 * MOVE_COOLDOWN) * 10,
				(int) (9.9 * MOVE_COOLDOWN) * 10,
				(int) (10.0 * MOVE_COOLDOWN) * 10,
				(int) (10.1 * MOVE_COOLDOWN) * 10,
				(int) (10.2 * MOVE_COOLDOWN) * 10,
				(int) (10.3 * MOVE_COOLDOWN) * 10,
				(int) (10.4 * MOVE_COOLDOWN) * 10,
				(int) (10.5 * MOVE_COOLDOWN) * 10,
				(int) (10.6 * MOVE_COOLDOWN) * 10,
				(int) (10.7 * MOVE_COOLDOWN) * 10,
				(int) (10.8 * MOVE_COOLDOWN) * 10,
				(int) (10.9 * MOVE_COOLDOWN) * 10,
				(int) (11.0 * MOVE_COOLDOWN) * 10};
	}
	
	public MoveResult move(MapLocation target_loc) throws GameActionException
	{
		if (!rc.isMovementReady())
			return MoveResult.FAIL;
		if (target_loc == null)
			return MoveResult.IMPOSSIBLE;
		int tar_loc_x = target_loc.x;
		int tar_loc_y = target_loc.y;
		if (tar_loc_x < 0 || tar_loc_y < 0
				|| tar_loc_x > X_BOUND || tar_loc_y > Y_BOUND)
			return MoveResult.IMPOSSIBLE;
		MapLocation current_loc = rc.getLocation();
		int cur_loc_x = current_loc.x;
		int cur_loc_y = current_loc.y;
		if (current_loc == target_loc)
			return MoveResult.REACHED;
		//System.out.printf("Check Complete, left = %d\n", Clock.getBytecodesLeft());
)");}
void printTail(){printf(R"(	}
})");}
#include <algorithm>
#include <string>
const int arrayOffset=15;
int lenVarName[309],DX[309],DY[309],xyToID[39][39],neighbour[309][19],lenNeighbour[309],dist[309];
char varName[309][109];
string eightDirections[]={"","SOUTHWEST","WEST","NORTHWEST","SOUTH","NORTH","SOUTHEAST","EAST","NORTHEAST"};
pair<int,int> dist_id[309];
int N;
void tab(){printf("		");}
void declareToBeZero(int x){tab();printf("int %s = 0;\n",varName[x]);}
string marbleLookupString(int x)
{
	return "MARBLE_COST_LOOKUP[rc.senseRubble(current_loc.translate("+to_string(DX[x])+","+to_string(DY[x])+"))]";
}
string marbleCostString(int x)
{
	return "((rc.canSenseLocation(current_loc.translate("+to_string(DX[x])+","+to_string(DY[x])+"))) ? ("+marbleLookupString(x)+") : (INF_MARBLE))";
}
void declareUponCanMove(int x,int y)
{
	tab();
	printf("int %s = (rc.canMove(%s)) ? (%s + %d) : (INF_MARBLE);\n",varName[x],eightDirections[y].c_str(),marbleCostString(x).c_str(),y);
}
void declareUponDP(int x)
{
	if (lenNeighbour[x]==1)
	{
		tab();
		printf("int %s = %s + %s;\n",varName[x],varName[neighbour[x][1]],marbleCostString(x).c_str());
	}
	else if (lenNeighbour[x]==2)
	{
		tab();
		printf("int %s = Math.min(%s,%s) + %s;\n",varName[x],varName[neighbour[x][1]],varName[neighbour[x][2]],marbleCostString(x).c_str());
	}
	else if (lenNeighbour[x]==3)
	{
		tab();
		printf("int %s = Math.min(Math.min(%s,%s),%s) + %s;\n",varName[x],
		varName[neighbour[x][1]],varName[neighbour[x][2]],varName[neighbour[x][3]],
		marbleCostString(x).c_str());
	}
	else
	{
		printf("??????????????\n");
	}
}
void getNodes()
{
	N=0;
	int L;
	for (L=vision;L*L>vision;L--);
	for (int x=-L;x<=L;x++)
		for (int y=-L;y<=L;y++)
			if (x*x+y*y<=vision)
			{
				N++;
				DX[N]=x,DY[N]=y;
				xyToID[x+arrayOffset][y+arrayOffset]=N;
				string str_x=to_string(x);
				std::replace( str_x.begin(), str_x.end(), '-', 'n');
				string str_y=to_string(y);
				std::replace( str_y.begin(), str_y.end(), '-', 'n');
				string full_string="d_"+str_x+"_"+str_y;
    			strcpy(varName[N], full_string.c_str());
    			lenVarName[N]=full_string.length();
    			dist_id[N]=make_pair(max(abs(x),abs(y)),N);
    			dist[N]=max(abs(x),abs(y));
			}
	sort(dist_id+1,dist_id+N+1);
}
void calculateDP()
{
	for (int i=1;i<=N;i++)
	{
		int currentID=dist_id[i].second;
		int directionID;
		if (i==1) declareToBeZero(currentID);
		else if (i>=2 && i<=9)
		{
			directionID=i-1;
			declareUponCanMove(currentID,directionID);
		}
		else if (i>=10)
		{
			for (int dx=-1;dx<=1;dx++)
				for (int dy=-1;dy<=1;dy++)
				{
					int NX=DX[currentID]+dx;
					int NY=DY[currentID]+dy;
					int NID=xyToID[NX+arrayOffset][NY+arrayOffset];
					if (!NID) continue;
					if (dist[NID]<dist[currentID])
						neighbour[currentID][++lenNeighbour[currentID]]=NID;
				}
			declareUponDP(currentID);
		}
	}
}
void declareAnswer()
{
	tab();
	printf("int minDistance = 1000000000;\n");
}
void minWithAnswer(int x)
{
	tab();
	printf("minDistance = Math.min(minDistance, %s + UPPER_BOUND_TURN_PER_GRID * Math.max(Math.abs(%d + dctx), Math.abs(%d + dcty)));\n",
	varName[x],DX[x],DY[x]);
}
void findAnswer()
{
	tab();
	printf("int dctx = cur_loc_x - tar_loc_x;\n");
	tab();
	printf("int dcty = cur_loc_y - tar_loc_y;\n");
	for (int i=1;i<=N;i++)
		minWithAnswer(i);
}
void tab3(){printf("			");}
void tab4(){printf("				");}
void tab5(){printf("					");}
void finalTrace()
{
	tab();
	printf("if (minDistance >= INF_MARBLE)\n");
	tab3();
	printf("return MoveResult.FAIL;\n");
	tab();
	printf("switch (minDistance % 10)\n");
	tab();
	printf("{\n");
	for (int i=1;i<=8;i++)
	{
		tab3();printf("case %d:\n",i);
		tab4();printf("if (rc.canMove(%s))\n",eightDirections[i].c_str());
		tab4();printf("{\n");
		tab5();printf("rc.move(%s);\n",eightDirections[i].c_str());
		tab5();printf("return MoveResult.SUCCESS;\n");
		tab4();printf("}\n");
		tab4();printf("else\n");
		tab5();printf("return MoveResult.FAIL;\n");
	}
	tab3();printf("default:\n");
	tab4();printf("return MoveResult.FAIL;\n");
	tab();
	printf("}\n");
}
int main()
{
	vision = 20;
	freopen("Code.txt","w",stdout);
	printHead();
	getNodes();
	calculateDP();
	declareAnswer();
	findAnswer();
	finalTrace();
	printTail();
	return 0;
}
