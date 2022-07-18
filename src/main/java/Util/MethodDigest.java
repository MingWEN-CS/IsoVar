package Util;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MethodDigest {
    public List<List<Unit>> realUnitsPaths = new LinkedList<>();

    public MethodDigest(Body body) {
        enumMethodPath(body);
    }

    public void enumMethodPath(Body body) {//Traversing paths and filtering by predicate
        BriefBlockGraph bg = new BriefBlockGraph(body);
//        modifyBlockGraph(bg, body);
        int num = bg.getBlocks().size();
        boolean[][] flags = new boolean[num][num];
        List<Block> heads = bg.getHeads();
        Block head = heads.get(0);
        List<Unit> unitPath = new ArrayList<>();
        traverseBlock(unitPath, head, flags);
    }

    public void traverseBlock(List<Unit> unitPath,
                              Block head, boolean[][] flags) {
        unitPath.addAll(getBlockUnits(head));//Add the information in the head to the list
        List<Block> nextBlocks = head.getSuccs();
        if (nextBlocks.isEmpty()) {
            this.realUnitsPaths.add(unitPath);
            return;
        }
        if (checkUnitIsPredicate(head.getTail())) {//If the block does not contain a predicate, you can save space by not saving the snapshot
            for (Block block : nextBlocks) {
                if (checkCycle(head, block, flags) == 1)
                    continue;
                List<Unit> newUnitPath = new ArrayList<>(unitPath);
                traverseBlock(newUnitPath, block, flags);
                flags[head.getIndexInMethod()][block.getIndexInMethod()] = false;
            }
        } else {
            Block block = nextBlocks.get(0);
            if (checkCycle(head, block, flags) == 1)
                return;
            traverseBlock(unitPath ,block, flags );
            flags[head.getIndexInMethod()][block.getIndexInMethod()] = false;
        }
    }

    public List<Unit> getBlockUnits(Block block) {
        List<Unit> paths = new ArrayList<>();
        for (Unit unit : block) {
            paths.add(unit);
        }
        return paths;
    }

    public int checkCycle(Block head, Block nextBlock, boolean[][] flags) {//Determine if the loop needs to be terminated
        int headIndex = head.getIndexInMethod();
        int nextIndex = nextBlock.getIndexInMethod();
        if (flags[headIndex][nextIndex])
            return 1;
        flags[headIndex][nextIndex] = true;
        return 0;
    }

    public boolean checkUnitIsPredicate(Unit unit) {
        return unit instanceof IfStmt || unit instanceof JLookupSwitchStmt || unit instanceof JTableSwitchStmt;
    }
}