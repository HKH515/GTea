package graphtea.extensions.reports.planarity.planaritypq.pqtree;

import graphtea.extensions.reports.planarity.planaritypq.pqtree.helpers.PQHelpers;
import graphtea.extensions.reports.planarity.planaritypq.pqtree.pqnodes.PNode;
import graphtea.extensions.reports.planarity.planaritypq.pqtree.pqnodes.PQNode;
import graphtea.extensions.reports.planarity.planaritypq.pqtree.pqnodes.QNode;

import java.util.*;

import static graphtea.extensions.reports.planarity.planaritypq.pqtree.helpers.PQHelpers.*;

/**
 * This class contains subroutines used to construct a PQ-Tree as described
 * in the 1976 paper "Testing for Consecutive Ones Property, Interval Graphs,
 * and Graph Planarity Using PQ-Tree Algorithms" by Kellogg S. Booth and George S. Lueker.
 *
 * We highly recommend reading over the paper before reading this codebase.
 *
 * @author Alex Cregten
 * @author Hannes Kr. Hannesson
 */

public class PQTree {

    /**
     * This subroutine determines which nodes of the PQ-Tree should be pruned
     * with respect to the constraint sequence S.
     *
     * @param _root root of the tree/subtree
     * @param S a set of nodes, describes a constraint sequence
     * @return a tree rooted at _root, where the children have the pertinentChildCount set.
     */
    public PNode bubble(PNode _root, List<PQNode> S){
        Queue<PQNode> queue = new LinkedList<>(S);
        int blockCount = 0;
        int blockedNodes = 0;
        int offTheTop = 0;

        while(queue.size() + blockCount + offTheTop > 1){
            if(queue.size() == 0){
                _root = null;
                return _root;
            }

            PQNode x = queue.remove();
            x.blocked = true;

            List<PQNode> BS = new ArrayList<>();
            List<PQNode> US = new ArrayList<>();

            // Only QNodes have immediate siblings
            for(PQNode u : x.immediateSiblings(true)){
                if(u == null){
                    continue;
                }
                if(u.blocked){
                    BS.add(u);
                }
                else {
                    US.add(u);
                }
            }

            if(US.size() > 0){
                PQNode y = US.get(0);
                x.setParent(y.getParent());
                x.blocked = false;
            }
            else if(x.immediateSiblings(true).size() < 2){
                x.blocked = false;
            }

            int listSize = 0;
            if(!x.blocked){
                PQNode y = x.getParent();
                if(BS.size() > 0){

                    Set<PQNode> list = x.maximalConsecutiveSetOfSiblingsAdjacent(true);
                    if(list != null){
                        listSize = list.size();
                        for(PQNode z : list){
                            z.blocked = false;
                            y.setPertinentChildCount(y.getPertinentChildCount() + 1);
                        }
                    }

                }
                if(y == null){
                    offTheTop = 1;
                }
                else {
                    y.setPertinentChildCount(y.getPertinentChildCount() + 1);
                    if(!queue.contains(y)){
                        queue.add(y);
                    }
                }
                blockCount = blockCount - BS.size();
                blockedNodes = blockedNodes - listSize;
            }
            else {
                blockCount = blockCount + 1 - BS.size();
                blockedNodes = blockedNodes + 1;
            }

        }

        return _root;
    }

    /**
     * This subroutine does the actual reduction on the tree, such that the resulting tree adheres to the constraint S.
     * This is done by matching the tree to "templates", which are essentially types of trees.
     * After matching the tree to a template, we can replace it with the template's replacement.
     * @param T root of the tree/subtree to reduce
     * @param S a set of nodes, describes a constraint sequence
     * @return a tree rooted at _root which do not violate S (nor the reverse of S) nor previously applied constraint sequences, if a matching
     * does not exist, returns the null tree.
     */
    public PNode reduce(PNode T, List<PQNode> S){
        Queue<PQNode> queue = new LinkedList<>(S);
        for(PQNode x : S){
            x.setPertinentLeafCount(1);
        }
        while(queue.size() > 0){
            PQNode x = queue.remove();
            if(x.getPertinentLeafCount() < S.size()){
                // X is not ROOT(T, S)

                PQNode y = x.getParent();
                y.setPertinentLeafCount(y.getPertinentLeafCount() + x.getPertinentLeafCount());
                y.setPertinentChildCount(y.getPertinentChildCount() - 1);
                if(y.getPertinentChildCount() == 0){
                    queue.add(y);
                }

                if(TEMPLATE_L1(x)) continue;
                if(TEMPLATE_P1(x)) continue;
                if(TEMPLATE_P3(x)) continue;
                if(TEMPLATE_P5(x)) continue;
                if(TEMPLATE_Q1(x)) continue;
                if(TEMPLATE_Q2(x)) continue;

                // If all templates fail, return null tree
                return null;
            }
            else {
                // X is ROOT(T, S)

                if(TEMPLATE_L1(x)) continue;
                if(TEMPLATE_P1(x)) continue;
                if(TEMPLATE_P2(x)) continue;
                if(TEMPLATE_P4(x)) continue;
                if(TEMPLATE_P6(x)) continue;
                if(TEMPLATE_Q1(x)) continue;
                if(TEMPLATE_Q2(x)) continue;
                if(TEMPLATE_Q3(x)) continue;

                // If all templates fail, return null tree
                return null;
            }

        }

        return T;
    }

    public void replace(PQNode T, PQNode TPrime){

    }

    /**
     * For each element s in S, s_i's ancestors are traversed up to a maximum of T.
     * If, for some j, the element ANCESTOR(s_i) is in the set ANCESTORS(s_j), s_i's traversal stops because that ancestor has already been found.
     * For each s_i, s_j that has a common ancestor, that ancestor is added to a list.  That list then calculates
     * the lowest depth ancestor which will be returned.
     *
     * @param T describes the root of the entire tree
     * @param S is the list of nodes that must be legally reachable
     * @return the root of the subtree that can reach all of S */
    public PQNode root(PQNode T, List<PQNode> S){
        if(S.size() == 0){
            return null;
        }

        if(S.size() == 1){
            return S.get(0);
        }

        List<PQNode> nodesReached = new ArrayList<>();
        List<PQNode> nodesSame = new ArrayList<>();

        for(PQNode s : S){
            PQNode traversal = s;
            while(traversal != T){
                traversal = traversal.getParent();
                if(nodesReached.contains(traversal)){
                    nodesSame.add(traversal);
                    break;
                }
                nodesReached.add(traversal);
            }
        }

        PQNode lowestDepthNode = null;
        int lowestDepth = Integer.MAX_VALUE;
        for(PQNode n : nodesSame){
            PQNode traversal = n;
            int curNodesLowestDepth = 0;
            while(traversal != T){
                traversal = traversal.getParent();
                curNodesLowestDepth++;
            }
            if(lowestDepth > curNodesLowestDepth){
                lowestDepthNode = n;
                lowestDepth = curNodesLowestDepth;
            }
        }

        if(lowestDepthNode == null){
            return nodesSame.get(0);
        }
        else {
            return lowestDepthNode;
        }

    }

    /**
    * TEMPLATES
    * */

    public boolean TEMPLATE_L1(PQNode x){
        if (x.getClass() == PNode.class || x.getClass() == QNode.class) {
            return false;
        }
        return true;
    }

    /**All children must be labelled identically (page 348)*/
    public boolean QNODE_TEMPLATE_1(PQNode x){
        PQNode front = x.endmostChildren().get(0);

        final String consistentLabel = front.getLabel();
        PQNode iter = front;

        do {
            if(iter.getLabel().equals(consistentLabel)){
                iter = iter.getCircularLink_next();
            }
            else {
                return false;
            }

        } while (iter != front);

        x.setLabel(consistentLabel);
        return true;
    }

    public boolean GENERALIZED_TEMPLATE_1(PQNode x){
        if(!x.getLabel().equals(PQNode.FULL)){
            for(PQNode n : x.getChildren()){
                if(!n.getLabel().equals(PQNode.FULL)){
                    return false;
                }
            }
            x.setLabel(PQNode.FULL);

            return true;
        }
        return false;
    }

    /**
     * Tries to match the tree/subtree at x to template P1, if successful,
     * we apply it.
     *
     * Case #1:            Case #2:
     * TEMPLATE:
     *    ----(E)---      |    ----(E)---
     *    |        |      |    |        |
     *                    |
     *    E  ....  E      |    F  ....  F
     *                    |
     * REPLACEMENT:       |
     *    ----(E)---      |    ----(F)---
     *    |        |      |    |        |
     *                    |
     *    E  ....  E      |    F  ....  F
     *                    |
     * @param x the node which represents the root of the tree/subtree
     * @return whether or not x matches the template
     */
    public boolean TEMPLATE_P1(PQNode x){
        if (x.getClass() == PNode.class) {
           return GENERALIZED_TEMPLATE_1(x);
       }
       return false;
    }

    /**
     * Tries to match the tree at x (x must be the root of the pertinent subtree) to template P2, if successful,
     * we apply it.
     *
     * TEMPLATE:
     *
     *    -------------(P)------------
     *    |        |        |        |
     *
     *    E  ....  E        F  ....  F
     *
     * REPLACEMENT:
     *    -------------(P)--------
     *    |        |             |
     *
     *    E  ....  E            (F)
     *                      |        |
     *
     *                      F  ....  F
     *
     * @param x the node which represents the root of the subtree
     * @return whether or not x matches the template
     */
    public boolean TEMPLATE_P2(PQNode x) {

        //Matching Phase

        if (x.getClass() != PNode.class) {
            return false;
        }

        List<PQNode> emptyChildren = new ArrayList<PQNode>();
        List<PQNode> fullChildren = new ArrayList<PQNode>();

        PQHelpers.collectChildrenByLabel(x, emptyChildren, fullChildren);


        //If there are no full nodes
        if (fullChildren.size() == 0) {
            return false;
        }
        //If there are no empty nodes
        if (emptyChildren.size() == 0) {
            return false;
        }
        //If there were other nodes than full or empty
        if ( fullChildren.size() + emptyChildren.size() != x.getChildren().size()) {
            return false;
        }

        // One full child
        if(fullChildren.size() == 1){

            return true;
        }

        //Replacement phase
        PNode fullParent = new PNode(PQNode.FULL);
        fullParent.setParent(x);

        //Add new pNode to root children list
        x.getChildren().add(fullParent);

        //Adding the full children to a new P node
        fullParent.addChildren(fullChildren);
        x.removeChildren(fullChildren);


        return true;
    }

    /**
     * Tries to match the subtree at x (x must NOT be the root of the pertinent subtree) to template P3, if successful,
     * we apply it.
     *
     * Note: This case is very similiar to TEMPLATE_P2.
     *       The matching is nearly identical, the only different being that x cannot be a root.
     *
     * TEMPLATE:
     *
     *    -------------(P)------------
     *    |        |        |        |
     *
     *    E  ....  E        F  ....  F
     *
     * REPLACEMENT:
     *       ---------[P]---------
     *       |                   |
     *
     *  ----(E)---          ----(F)---
     *  |        |          |        |
     *
     *  F  ....  F          F  ....  F
     *
     * @param x the node which represents the root of the tree/subtree
     * @return whether or not x matches the template
     */
    public boolean TEMPLATE_P3(PQNode x){

        //Matching Phase
        if (x.getClass() != PNode.class) {
            return false;
        }

        List<PQNode> emptyChildren = new ArrayList<PQNode>();
        List<PQNode> fullChildren = new ArrayList<PQNode>();

        PQHelpers.collectChildrenByLabel(x, emptyChildren, fullChildren);

        //If there are no full nodes
        if (fullChildren.size() == 0) {
            return false;
        }
        //If there are no empty nodes
        if (emptyChildren.size() == 0) {
            return false;
        }
        //If there were other nodes than full or empty
        if ( fullChildren.size() + emptyChildren.size() != ((PNode) x).getChildren().size()) {
            return false;
        }

        //Replacement phase
        PQNode xQ = new QNode(PQNode.PARTIAL);
        PQHelpers.replaceParent(xQ, x);

        // Alternative form B
        if(emptyChildren.size() == 1 && fullChildren.size() == 1){
            PQNode emptyNode = emptyChildren.get(0);
            PQNode fullNode = fullChildren.get(0);
            setCircularLinks(Arrays.asList(emptyNode, fullNode));
            xQ.setQNodeEndmostChildren(emptyNode, fullNode);
            xQ.setParentQNodeChildren();

            return true;
        }

        PQNode emptyPNode = new PNode(PQNode.EMPTY);
        PQNode fullPNode = new PNode(PQNode.FULL);

        emptyPNode.setParent(xQ);
        fullPNode.setParent(xQ);

        //Adding the children to the appropriate P node
        emptyPNode.addChildren(emptyChildren);
        fullPNode.addChildren(fullChildren);

        if(emptyChildren.size() == 1 && fullChildren.size() > 1){
            PQNode emptyChild = emptyChildren.get(0);
            setCircularLinks(Arrays.asList(fullPNode, emptyChild));
            xQ.setQNodeEndmostChildren(fullPNode, emptyChild);
            xQ.setParentQNodeChildren();

        }
        else if(emptyChildren.size() > 1 && fullChildren.size() == 1){
            PQNode fullChild = fullChildren.get(0);
            setCircularLinks(Arrays.asList(emptyPNode, fullChild));
            xQ.setQNodeEndmostChildren(emptyPNode, fullChild);
            xQ.setParentQNodeChildren();
        }
        else {
            setCircularLinks(Arrays.asList(emptyPNode, fullPNode));
            setCircularLinks(Arrays.asList(emptyPNode, fullPNode));
            xQ.setQNodeEndmostChildren(emptyPNode, fullPNode);
            xQ.setParentQNodeChildren();
        }

        xQ.setParentQNodeChildren();

        return true;
    }


    /**
     * Tries to match the tree at x (x must be the root of the pertinent subtree) to template P4, if successful,
     * we apply it.
     *
     * TEMPLATE:
     *
     *    -----------------------(P)-----------------------
     *    |      |                |                |      |
     *
     *    E .... E       --------[P]--------       F .... F
     *                   |      |   |      |
     *
     *                   E .... E   F .... F
     *
     * REPLACEMENT:
     *    ----------------------(P)----------------
     *    |        |                              |
     *
     *    E  ....  E               ---------------[P]----------------
     *                             |        |   |        |          |
     *
     *                             E  ....  E   F  ....  F     ----(F)---
     *                                                         |        |
     *
     *                                                         F  ....  F
     *
     * @param x the node which represents the root of the tree/subtree
     * @return whether or not x matches the template
     */
    public boolean TEMPLATE_P4(PQNode x){

        if (x.getClass() != PNode.class) {
            return false;
        }

        //Matching phase
        List<PQNode> emptyChildren = x.getChildrenOfLabel(PQNode.EMPTY);
        List<PQNode> fullChildren = x.getChildrenOfLabel(PQNode.FULL);
        List<PQNode> partialChildren = x.getChildrenOfLabel(PQNode.PARTIAL);

        //If there is not exactly 1 partial child
        if (partialChildren.size() != 1) {
            return false;
        }

        //If there were other nodes than full, empty or partial
        if ( fullChildren.size() + emptyChildren.size() + partialChildren.size() != x.getChildren().size()) {
            return false;
        }

        //If partial node is not a Q node
        if (partialChildren.get(0).getClass() != QNode.class) {
            return false;
        }

        PQNode partialNode = partialChildren.get(0);

        //If full children are not consecutive
        if (!checkIfConsecutive(partialNode.getChildrenOfLabel(PQNode.FULL))) {
            return false;
        }

        //If empty children are not consecutive
        if (!checkIfConsecutive(partialNode.getChildrenOfLabel(PQNode.EMPTY))) {
            return false;
        }

        //Replacement phase
        QNode qNode = (QNode) partialChildren.get(0);
        PQNode leftMost = qNode.endmostChildren().get(0);
        PQNode rightMost = qNode.endmostChildren().get(1);
        x.removeChildren(fullChildren);

        if(fullChildren.size() == 1){
            PQNode fullChild = fullChildren.get(0);


            fullChild.setParent(qNode);

            // Add child to side of Q-Node that is not empty
            if(!qNode.endmostChildren().get(0).getLabel().equals(PQNode.EMPTY)){
                PQHelpers.insertNodeIntoCircularList(fullChild, leftMost, rightMost);
                qNode.setQNodeEndmostChildren(fullChild, null);
                qNode.setParentQNodeChildren();

            }
            else {
                PQHelpers.insertNodeIntoCircularList(fullChild, rightMost, leftMost);
                qNode.setQNodeEndmostChildren(null, fullChild);
                qNode.setParentQNodeChildren();
            }
        }
        else {
            PQNode pNodeParent = new PNode(PQNode.FULL);
            pNodeParent.addChildren(fullChildren);
            pNodeParent.setParent(qNode);


            // Add child to side of Q-Node that is not empty
            if(!qNode.endmostChildren().get(0).getLabel().equals(PQNode.EMPTY)){
                PQHelpers.insertNodeIntoCircularList(pNodeParent, leftMost, rightMost);
                qNode.setQNodeEndmostChildren(pNodeParent, null);
                qNode.setParentQNodeChildren();
            }
            else {
                PQHelpers.insertNodeIntoCircularList(pNodeParent, rightMost, leftMost);
                qNode.setQNodeEndmostChildren(null, pNodeParent);
                qNode.setParentQNodeChildren();
            }
        }

        PQNode xParent = x.getParent();
        if(x.getChildren().size() == 1 && xParent != null) {
            if (xParent.getClass() == PNode.class) {
                xParent.replaceChild(qNode, x);
            } else {
                xParent.replaceChild(qNode, x);
            }
        }

        qNode.setParentQNodeChildren();


        return true;
    }

    /**
     * Tries to match the subtree at x (x must NOT be the root of the pertinent subtree) to template P5, if successful,
     * we apply it.
     * Note: This case is very similar to TEMPLATE_P4.
     *       The matching is nearly identical, the only different being that x cannot be a root.
     * TEMPLATE:
     *
     *    -----------------------(P)-----------------------
     *    |      |                |                |      |
     *
     *    E .... E       --------[P]--------       F .... F
     *                   |      |   |      |
     *
     *                   E .... E   F .... F
     *
     * REPLACEMENT:
     *    -----------------------[P]-------------------
     *       |           |      |   |       |         |
     *
     *   ---(E)--        E .... E   F .... F      ---(F)--
     *   |      |                                 |      |
     *
     *   E .... E                                 F .... F
     *
     * @param x the node which represents the root of the tree/subtree
     * @return whether or not x matches the template
     */
    public boolean TEMPLATE_P5(PQNode x){

        if (x.getClass() != PNode.class) {
            return false;
        }

        if(x.getChildren().size() == 0){
            return false;
        }

        PQNode qNode = null;
        int qNodeCount = 0;
        List<PQNode> emptyChildList = new ArrayList<>();
        List<PQNode> fullChildList = new ArrayList<>();
        for(PQNode n : x.getChildren()){
            if (n.getClass() == QNode.class) {
                qNodeCount++;
                qNode = n;
            }
            else if(n.getLabel().equals(PQNode.EMPTY)){
                emptyChildList.add(n);
            }
            else if(n.getLabel().equals(PQNode.FULL)){
                fullChildList.add(n);
            }
        }

        if(qNodeCount != 1 || !qNode.getLabel().equals(PQNode.PARTIAL)){
            return false;
        }

        PQNode newEmptiesNode = null;
        PQNode newFullsNode = null;

        if(emptyChildList.size() > 0) {
            if (emptyChildList.size() == 1) {
                newEmptiesNode = emptyChildList.get(0);
            }
            else {
                newEmptiesNode = new PNode(PQNode.EMPTY);
                newEmptiesNode.addChildren(emptyChildList);
            }
            PQHelpers.addNodesAsChildrenToQNode(Arrays.asList(newEmptiesNode), (QNode) qNode);
        }

        if(fullChildList.size() > 0) {
            if (fullChildList.size() == 1) {
                newFullsNode = fullChildList.get(0);

            }
            else {
                newFullsNode = new PNode(PQNode.FULL);
                newFullsNode.addChildren(fullChildList);
            }
            PQHelpers.addNodesAsChildrenToQNode(Arrays.asList(newFullsNode), (QNode) qNode);
        }

        PQNode xParent = x.getParent();
        qNode.setParent(xParent);
        if (xParent.getClass() == PNode.class) {
            xParent.replaceChild(qNode, x);
        }
        else {
            xParent.replaceChild(qNode, x);
        }

        qNode.setParentQNodeChildren();


        return true;
    }

    /**
     * Tries to match the tree at x (x must be the root of the pertinent subtree) to template P6, if successful,
     * we apply it.
     *
     * TEMPLATE:
     *
     *    --------------------------------(P)---------------------------------
     *    |      |                |                |      |                  |
     *
     *    E .... E       --------[P]--------       F .... F         --------[P]--------
     *                   |      |   |      |                        |      |   |      |
     *
     *                   E .... E   F .... F                        F .... F   E .... E
     *
     * REPLACEMENT:
     *    --------------------------------(P)-----------
     *    |      |                                     |
     *
     *    E .... E       -----------------------------[P]------------------------------
     *                   |      |   |      |           |            |      |   |      |
     *
     *                   E .... E   F .... F       ---(F)--        F .... F   E .... E
     *                                             |      |
     *
     *                                             F .... F
     *
     * @param x the node which represents the root of the tree/subtree
     * @return whether or not x matches the template
     */

    public boolean TEMPLATE_P6(PQNode x){

        /** Matching the template */
        if (x.getClass() != PNode.class) {
            return false;
        }

        if(x.getChildren().size() == 0){
            return false;
        }

        /** Gather root children */
        QNode qNode1 = null;
        QNode qNode2 = null;
        int qNodeCount = 0;
        List<PQNode> emptyRootChildList = new ArrayList<>();
        List<PQNode> fullRootChildList = new ArrayList<>();
        for(PQNode n : x.getChildren()){
            if (n.getClass() == QNode.class) {
                if(qNodeCount == 0){
                    qNode1 = (QNode) n;
                    qNodeCount++;
                }
                else if(qNodeCount == 1){
                    qNode2 = (QNode) n;
                    qNodeCount++;
                }
                else {
                    return false;
                }
            }
            else if(n.getLabel().equals(PQNode.EMPTY)){
                emptyRootChildList.add(n);
            }
            else if(n.getLabel().equals(PQNode.FULL)){
                fullRootChildList.add(n);
            }
        }

        if(qNode1 == null || qNode2 == null){
            return false;
        }
        if(!qNode1.getLabel().equals(PQNode.PARTIAL) || !qNode2.getLabel().equals(PQNode.PARTIAL)){
            return false;
        }
        if(qNode1.endmostChildren().size() != 2 || qNode2.endmostChildren().size() != 2){
            return false;
        }

        /** Gather qNode1 children */
        List<PQNode> emptyQNodeChildList1 = new ArrayList<>();
        List<PQNode> fullQNodeChildList1 = new ArrayList<>();
        gatherQNodeChildren(emptyQNodeChildList1, fullQNodeChildList1, qNode1);

        //If empty children are not consecutive
        if (!checkIfConsecutive( emptyQNodeChildList1 )) {
            return false;
        }

        //If full children are not consecutive
        if (!checkIfConsecutive(fullQNodeChildList1)) {
            return false;
        }

        /** Gather qNode2 children */
        List<PQNode> emptyQNodeChildList2 = new ArrayList<>();
        List<PQNode> fullQNodeChildList2 = new ArrayList<>();
        gatherQNodeChildren(emptyQNodeChildList2, fullQNodeChildList2, qNode2);

        //If empty children are not consecutive
        if (!checkIfConsecutive( emptyQNodeChildList2 )) {
            return false;
        }

        //If full children are not consecutive
        if (!checkIfConsecutive(fullQNodeChildList2)) {
            return false;
        }


        /**
         *  Applying the template
         * */

        /** Setup PNode */
        PQNode pNode = new PNode(PQNode.FULL);
        if(fullRootChildList.size() > 1) {
            pNode.addChildren(fullRootChildList);
        }

        /** Reconfigure qNode1 */
        PQNode leftMost1 = qNode1.endmostChildren().get(0);
        PQNode rightMost1 = qNode1.endmostChildren().get(1);
        if(leftMost1.getLabel().equals(PQNode.FULL) && rightMost1.getLabel().equals(PQNode.EMPTY)){
            qNode1.rotate();
            qNode1.rotate();
            leftMost1 = qNode1.endmostChildren().get(0);
            rightMost1 = qNode1.endmostChildren().get(1);
        }

        /** Reconfigure qNode2 */
        PQNode leftMost2 = qNode2.endmostChildren().get(0);
        PQNode rightMost2 = qNode2.endmostChildren().get(1);
        if(leftMost2.getLabel().equals(PQNode.EMPTY) && rightMost2.getLabel().equals(PQNode.FULL)){
            qNode2.rotate();
            leftMost2 = qNode2.endmostChildren().get(0);
            rightMost2 = qNode2.endmostChildren().get(1);
        }

        /** Reconfigure circular links for qNode1, pNode, qNode2
         * Could use setCircularLinks in  in PQHelper, but there is no need
         * to set all of the links. */
        rightMost2.setCircularLink_next(leftMost1);

        leftMost1.setCircularLink_prev(rightMost2);

        if(fullRootChildList.size() > 1){
            rightMost1.setCircularLink_next(pNode);
            pNode.setCircularLink_next(leftMost2);

            leftMost2.setCircularLink_prev(pNode);
            pNode.setCircularLink_prev(rightMost1);
        }
        else {
            rightMost1.setCircularLink_next(leftMost2);
            leftMost2.setCircularLink_prev(rightMost1);

            rightMost2.setCircularLink_next(leftMost1);
            leftMost1.setCircularLink_prev(rightMost2);
        }

        /** Reconfigure qNode1, qNode2 parents */
        rightMost1.setParent(null);
        leftMost2.setParent(null);

        /** Setup merging QNode */
        PQNode mergingQNode = new QNode(PQNode.PARTIAL);

        // Simplify if x only has one child
        PQNode xParent = x.getParent();
        if(xParent != null){
            if (xParent.getClass() == QNode.class) {
                xParent.replaceChild(mergingQNode, x);
            }
            else {
                xParent.replaceChild(mergingQNode, x);
            }

        }
        else {
            mergingQNode.setParent(x);

            // Reconfigure root of whole tree
            x.removeChildren(fullRootChildList);
            x.removeChild(qNode1);
            x.removeChild(qNode2);
            x.addChild(mergingQNode);
        }


        mergingQNode.setQNodeEndmostChildren(leftMost1, rightMost2);
        mergingQNode.setParentQNodeChildren();


        return true;
    }


    /**
     * Tries to match the tree with root x to template Q1, if successful,
     * we apply it.
     *
     * TEMPLATE:
     * Case #1:           |Case #2:
     *        [E]         |        [E]
     *    |        |      |    |        |
     *                    |
     *    E  ....  E      |    F  ....  F
     *                    |
     * REPLACEMENT:       |
     *        [E]         |        [F]
     *    |        |      |    |        |
     *                    |
     *    E  ....  E      |    F  ....  F
     *                    |
     * @param x the node which represents the root of the tree/subtree
     * @return whether or not x matches the template
     */
    public boolean TEMPLATE_Q1(PQNode x){
        if (x.getClass() == QNode.class) {
           //return GENERALIZED_TEMPLATE_1(x);
           return QNODE_TEMPLATE_1(x);
       }
       return false;
    }

    /**
     * Tries to match the tree/subtree at x to template Q2, if successful,
     * we apply it.
     *
     * TEMPLATE:
     *
     *    -----------------------[P]-----------------------
     *    |      |                |                |      |
     *
     *    E .... E       --------[P]--------       F .... F
     *                   |      |   |      |
     *
     *                   E .... E   F .... F
     *
     * REPLACEMENT:
     *    -----------------------[P]------------------------
     *    |      |       |      |   |       |       |      |
     *
     *    E .... E       E .... E   F .... F        F .... F
     *
     * @param x the node which represents the root of the tree/subtree
     * @return whether or not x matches the template
     */
    public boolean TEMPLATE_Q2(PQNode x){

        //Matching

        //If x is not a qNode
        if (x.getClass() != QNode.class) {
            return false;
        }

        //If empty children are not consecutive
        //if (!checkIfConsecutive(x.getChildrenOfLabel(PQNode.EMPTY))) {
        //    return false;
        //}

        //If full children are not consecutive
        if (!checkIfConsecutive(x.getChildrenOfLabel(PQNode.FULL))) {
            return false;
        }

        //Check if x is not singly partial
        if (x.getChildrenOfLabel(PQNode.PARTIAL).size() > 1) {
            return false;
        }

        if(x.getChildrenOfLabel(PQNode.PARTIAL).size() == 1) {
            //Check if partial node is not a qnode
            if (x.getChildrenOfLabel(PQNode.PARTIAL).get(0).getClass() != QNode.class) {
                return false;
            }

            //Check if the partial node's full children are not consecutive
            if (!PQHelpers.checkIfConsecutive(x.getChildrenOfLabel(PQNode.PARTIAL).get(0).getChildrenOfLabel(PQNode.FULL))) {
                return false;
            }

            //Check if the partial node's empty children are not consecutive
            if (!PQHelpers.checkIfConsecutive(x.getChildrenOfLabel(PQNode.PARTIAL).get(0).getChildrenOfLabel(PQNode.EMPTY))) {
                return false;
            }
        }
        else {
            // No reduction needed
            x.setLabel(PQNode.PARTIAL);
            return true;
        }

        //Replacement
        QNode partialNode = (QNode) x.getChildrenOfLabel(PQNode.PARTIAL).get(0);

        x.setLabel(PQNode.PARTIAL);

        PQNode leftMostChildOfQ = partialNode.endmostChildren().get(0);
        PQNode rightMostChildOfQ = partialNode.endmostChildren().get(1);
        PQNode leftMostChildOfX = x.endmostChildren().get(0);
        PQNode rightMostChildOfX = x.endmostChildren().get(1);

        if(partialNode == leftMostChildOfX){
            // Q ... rest
            if(partialNode.getCircularLink_next().getLabel().equals(PQNode.FULL) && leftMostChildOfQ.getLabel().equals(PQNode.FULL)){
                // x children: (partialNode) ... (fulls) ... (empties)
                // partialNode children: (fulls) ... (empties) -> (empties) ... (fulls)
                partialNode.rotate();
            }
            else {
                // ignore
            }
        }
        else if(partialNode == rightMostChildOfX){
            // rest ... Q
            if(partialNode.getCircularLink_prev().getLabel().equals(PQNode.FULL) && rightMostChildOfQ.getLabel().equals(PQNode.FULL)){
                // x children: (empties) ... (fulls) ... (partialNode)
                // partialNode children: (empties) ... (fulls) -> (fulls) ... (empties)
                partialNode.rotate();
            }
            else {
                // ignore
            }
        }
        else {
            // some ... Q ... some
            if(partialNode.getCircularLink_prev().getLabel().equals(PQNode.FULL) && leftMostChildOfQ.getLabel().equals(PQNode.EMPTY)){
                // x children: (fulls) ... (partialNode) ... (empties)
                partialNode.rotate();
            }
            else {
                // ignore
            }

        }

        PQHelpers.reduceChildQNodeIntoParentQNode(partialNode, (QNode) x);

        x.setParentQNodeChildren();

        return true;
    }

    /**
     * Tries to match the tree at x (x must be the root of the pertinent subtree) to template P6, if successful,
     * we apply it.
     *
     * TEMPLATE:
     *
     *    --------------------------------------------[P]-----------------------------------------------
     *    |      |                |                |      |                  |                  |      |
     *
     *    E .... E       --------[P]--------       F .... F         --------[P]--------         E .... E
     *                   |      |   |      |                        |      |   |      |
     *
     *                   E .... E   F .... F                        F .... F   E .... E
     *
     * REPLACEMENT:
     *
     *    --------------------------------------------[P]--------------------------------------------------
     *    |      |       |      |       |      |      |      |       |      |       |      |       |      |
     *
     *    E .... E       E .... E       F .... F      F .... F       F .... F       E .... E       E .... E
     *
     * @param x the node which represents the root of the tree/subtree
     * @return whether or not x matches the template
     */
    public boolean TEMPLATE_Q3(PQNode x){

        //Matching phase

        //Check if not qnode
        if (x.getClass() != QNode.class) {
            return false;
        }

        //Check if x is not doubly partial
        if (x.getChildrenOfLabel(PQNode.PARTIAL).size() != 2) {
            return false;
        }

        //Check if partial nodes are not qnodes
        if (x.getChildrenOfLabel(PQNode.PARTIAL).get(0).getClass() != QNode.class) {
            return false;
        }
        if (x.getChildrenOfLabel(PQNode.PARTIAL).get(1).getClass() != QNode.class) {
            return false;
        }

        //Check if the partial node's full children are not consecutive
        if (!PQHelpers.checkIfConsecutive(x.getChildrenOfLabel(PQNode.PARTIAL).get(0).getChildrenOfLabel(PQNode.FULL))) {
            return false;
        }
        if (!PQHelpers.checkIfConsecutive(x.getChildrenOfLabel(PQNode.PARTIAL).get(1).getChildrenOfLabel(PQNode.FULL))) {
            return false;
        }

        //Check if the partial node's empty children are not consecutive
        if (!PQHelpers.checkIfConsecutive(x.getChildrenOfLabel(PQNode.PARTIAL).get(0).getChildrenOfLabel(PQNode.EMPTY))) {
            return false;
        }
        if (!PQHelpers.checkIfConsecutive(x.getChildrenOfLabel(PQNode.PARTIAL).get(1).getChildrenOfLabel(PQNode.EMPTY))) {
            return false;
        }

        List<PQNode> leftEmpties = new ArrayList<PQNode>();
        List<PQNode> rightEmpties = new ArrayList<PQNode>();

        int flips = 0;
        int cntr = 0;
        for (PQNode n : x.getChildren()) {
            cntr++;
            if (flips == 0) {
                if (n.getLabel().equals(PQNode.PARTIAL)) {
                    flips++;
                }
                else if (!n.getLabel().equals(PQNode.EMPTY)) {
                    return false;
                }
                //If empty
                else {
                   leftEmpties.add(n);
                }
            }
            else if (flips == 1) {
                if (n.getLabel().equals(PQNode.FULL)) {
                    flips++;
                }
                else if (!n.getLabel().equals(PQNode.PARTIAL)) {
                    return false;
                }
            }
            else if (flips == 2) {
                if (n.getLabel().equals(PQNode.PARTIAL)) {
                    flips++;
                }
                else if (!n.getLabel().equals(PQNode.FULL)) {
                    return false;
                }
            }
            else if (flips == 3) {
                if (n.getLabel().equals(PQNode.EMPTY)) {
                    flips++;
                    rightEmpties.add(n);
                }
                else if (!n.getLabel().equals(PQNode.PARTIAL)) {
                    return false;
                }
            }
            else {
                if (!n.getLabel().equals(PQNode.EMPTY)) {
                    return false;
                }
                //If empty
                else {
                    rightEmpties.add(n);
                }
            }
        }

        //Replacement phase

        x.setLabel(PQNode.PARTIAL);
        List<PQNode> partials = x.getChildrenOfLabel(PQNode.PARTIAL);
        QNode leftQNode = (QNode) partials.get(0);
        QNode rightQNode = (QNode) partials.get(1);
        PQNode leftMostChildOfX = x.endmostChildren().get(0);
        PQNode rightMostChildOfX = x.endmostChildren().get(1);

        if(!PQHelpers.rotateIfNeeded(leftQNode))
            return false;
        if(!PQHelpers.rotateIfNeeded(rightQNode)){
            return false;
        }


        List<PQNode> replacementChildren = new ArrayList<PQNode>();
        replacementChildren.addAll(leftEmpties);
        replacementChildren.addAll(partials.get(0).getChildren());
        replacementChildren.addAll(x.getChildrenOfLabel(PQNode.FULL));
        replacementChildren.addAll(partials.get(1).getChildren());
        replacementChildren.addAll(rightEmpties);

        leftEmpties.get(0).setParent(x);
        rightEmpties.get(rightEmpties.size() - 1).setParent(x);
        setCircularLinks(replacementChildren);

        x.setQNodeEndmostChildren(replacementChildren.get(0), replacementChildren.get(replacementChildren.size()-1));
        x.setParentQNodeChildren();

        return true;
    }

}





