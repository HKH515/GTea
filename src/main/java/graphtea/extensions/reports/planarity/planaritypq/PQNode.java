package graphtea.extensions.reports.planarity.planaritypq;

import java.util.*;

import graphtea.extensions.reports.planarity.planaritypq.IllegalNodeTypeException;

public class PQNode {
    static String PNODE = "p-node";
    static String QNODE = "q-node";
    static String PSEUDO_NODE = "pseudonode";
    String nodeType = ""; // update the type

    static String PARTIAL = "partial";
    static String FULL = "full";
    static String EMPTY = "EMPTY";
    String labelType = "";

    PQNode circularLink_next;
    PQNode circularLink_prev;

    int pertinentChildCount;

    int pertinentLeafCount;

    boolean queued;

    boolean blocked;

    //boolean marked;

    int childCount;

    PQNode parent;
    List<PQNode> children = new ArrayList<>();

    public PQNode(){
        blocked = false;
        //marked = false;
        pertinentChildCount = 0;
        pertinentLeafCount = 0;
        queued = false;
        circularLink_next = null;
        circularLink_prev = null;
        childCount = 0;
    }

    public boolean nodeType(String checkType){
        return nodeType.equals(checkType);
    }

    public List<PQNode> partialChildren() {
        List<PQNode> cList = new ArrayList<PQNode>();
        for (PQNode v : this.children) {
            if (v.labelType == PARTIAL) {
                cList.add(v);
            }
        }
        return cList;
    }

    public void setPartialChildren(List<PQNode> list){

    }

    /*
     * Note: only for q-nodes
     */
    public List<PQNode> endmostChildren(){
        try {
            if (this.nodeType != QNODE) {

                throw new IllegalNodeTypeException("endmostChildren() is only valid for Q-Nodes");
            }
            return this.children;
        }
        catch (IllegalNodeTypeException e) {

        }
        return null;
    }

    public PQNode emptySibling(){
        PQNode currNode = this.circularLink_next;
        while (true) {
            if (currNode.labelType == EMPTY && currNode != this) {
                return currNode;
            }
            else if (currNode == this) {
                return null;
            }
            currNode = currNode.circularLink_next;
        }
    }

    public void removeFromCircularLink(){
        this.circularLink_next.circularLink_prev = this.circularLink_prev;
        this.circularLink_prev.circularLink_next = this.circularLink_next;
        this.circularLink_next = null;
        this.circularLink_prev = null;
    }

    public List<PQNode> immediateSiblings(){
        List<PQNode> adjacents = new ArrayList<PQNode>();
        if(this.circularLink_prev != null)
            adjacents.add(this.circularLink_prev);
        if(this.circularLink_next != null)
            adjacents.add(this.circularLink_next);
        return adjacents;
    }

    public void replaceInCircularLink(PQNode x){
        this.circularLink_next.circularLink_prev = x;
        this.circularLink_prev.circularLink_next = x;
        x.circularLink_prev = this.circularLink_prev;
        x.circularLink_next = this.circularLink_next;
        this.circularLink_next = null;
        this.circularLink_prev = null;
    }

    public void replaceInImmediateSiblings(PQNode x, PQNode y){
        this.circularLink_prev = x;
        this.circularLink_next = y;
        // x is replaced by y
    }

    public void setEndmostChildren(List<PQNode> list){

    }

    public List<PQNode> fullChildren(){
        List<PQNode> full = new ArrayList<PQNode>();
        for (PQNode c : children) {
            if (c.labelType.equals(FULL)) {
                full.add(c);
            }
        }
        return full;
    }

    public void setImmediateSiblings(List<PQNode> list){

    }

    public PQNode getImmediateSiblingOfNodeType(String nodeType){
        if(this.circularLink_next.nodeType.equals(nodeType)){
            return this.circularLink_next;
        }
        else if(this.circularLink_prev.nodeType.equals(nodeType)){
            return this.circularLink_prev;
        }
        return null;
    }

    public PQNode endmostChild(String nodeType){
        return null;
    }


    /** todo: Check if we should be returning max{left, right}, or left union right
     *
     * Returns:
     * 11110[1111]
     *     ^
     * [11111111]00
     *      ^ (any of the 1's)
     * 00[111111111]
     *       ^ (any of the 1's)
     * 1100[111]000011
     *       ^ ()any of the three 1's)
     * 00001110000
     *  ^  (any of the four 0's)
     * 1110[11111]
     *    ^ (Max{left, right})
     * etc
     * */
    public Set<PQNode> maximalConsecutiveSetOfSiblingsAdjacent(boolean blocked){
        Set<PQNode> left = null;
        Set<PQNode> right = null;
        if(this.circularLink_prev != null) {
            left = this.circularLink_prev.siblingsAdjacent(blocked, true);
        }
        if(this.circularLink_next != null) {
            right = this.circularLink_next.siblingsAdjacent(blocked, false);
        }

        if(left == null && right != null){
            if(this.blocked == blocked) {
                right.add(this);
                return right;
            }
            return right;
        }
        else if(right == null && left != null){
            if(this.blocked == blocked){
                left.add(this);
                return left;
            }
            return left;
        }
        else if(left == null && right == null){
            return null;
        }

        if(this.blocked == blocked){
            left.add(this);
            right.add(this);
        }

        if(this.blocked == blocked) {
            left.addAll(right);
            return left;
        }
        else if(left.size() > right.size()){
            return left;
        }
        else {
            return right;
        }
    }

    private Set<PQNode> siblingsAdjacent(boolean blocked, boolean goLeft){

        if(this.blocked == blocked) {
            if(goLeft && this.circularLink_prev != null){
                try {
                    Set<PQNode> list = new HashSet<PQNode>(this.circularLink_prev.siblingsAdjacent(blocked, goLeft));
                    list.add(this);
                    return list;
                }
                catch (NullPointerException e){
                    return Collections.singleton(this);
                }
            }
            else if(this.circularLink_next != null){
                try {
                    Set<PQNode> list = new HashSet<>(this.circularLink_next.siblingsAdjacent(blocked, goLeft));
                    list.add(this);
                    return list;
                }
                catch (NullPointerException e){
                    return Collections.singleton(this);
                }
            }
            else {
                return Collections.singleton(this);
            }
        }
        else {
            return null;
        }
    }


}