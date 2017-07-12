/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ons;

import ons.ra.RA;
import ons.ra.ControlPlaneForRA;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The Control Plane is responsible for managing resources and
 * connection within the network.
 * 
 * @author onsteam
 */
public class ControlPlane implements ControlPlaneForRA { // RA is Routing Assignment Problem

    private RA ra;
    private PhysicalTopology pt;
    private VirtualTopology vt;
    private Map<Flow, Path> mappedFlows; // Flows that have been accepted into the network
    private Map<Long, Flow> activeFlows; // Flows that have been accepted or that are waiting for a RA decision 
    private Tracer tr = Tracer.getTracerObject();
    private MyStatistics st = MyStatistics.getMyStatisticsObject();

    /**
     * Creates a new ControlPlane object.
     * 
     * @param raModule the name of the RA class
     * @param pt the network's physical topology
     * @param vt the network's virtual topology
     */
    public ControlPlane(String raModule, PhysicalTopology pt, VirtualTopology vt) {
        Class RAClass;

        mappedFlows = new HashMap<Flow, Path>();
        activeFlows = new HashMap<Long, Flow>();

        this.pt = pt;
        this.vt = vt;

        try {
            RAClass = Class.forName(raModule);
            ra = (RA) RAClass.newInstance();
            ra.simulationInterface(this);
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /**
     * Deals with an Event from the event queue.
     * If it is of the FlowArrivalEvent kind, adds it to the list of active flows.
     * If it is from the FlowDepartureEvent, removes it from the list.
     * 
     * @param event the Event object taken from the queue 
     */
    public void newEvent(Event event) {

        if (event instanceof FlowArrivalEvent) {
            newFlow(((FlowArrivalEvent) event).getFlow());
            ra.flowArrival(((FlowArrivalEvent) event).getFlow());
        } else if (event instanceof FlowDepartureEvent) {
            ra.flowDeparture(((FlowDepartureEvent) event).getID());
            removeFlow(((FlowDepartureEvent) event).getID());
        }
    }

    /**
     * Adds a given active Flow object to a determined Physical Topology.
     * 
     * @param id unique identifier of the Flow object
     * @param lightpaths the Path, or list of LighPath objects
     * @return true if operation was successful, or false if a problem occurred
     */
    @Override
    public boolean acceptFlow(long id, LightPath[] lightpaths) {
        Flow flow;

        if (id < 0 || lightpaths.length < 1) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
                return false;
            }
            flow = activeFlows.get(id);
            if (!canAddFlowToPT(flow, lightpaths)) {
                return false;
            }
            if(!checkLightpathContinuity(flow, lightpaths)){
                return false;
            }
            int usedTransponders = 0;
            for (LightPath lightpath : lightpaths) {
                if(vt.isLightpathIdle(lightpath.getID())){
                    usedTransponders++;
                }
            }
            addFlowToPT(flow, lightpaths);
            mappedFlows.put(flow, new Path(lightpaths));
            tr.acceptFlow(flow, lightpaths);
            st.userTransponder(usedTransponders);
            st.acceptFlow(flow, lightpaths);
            return true;
        }
    }

    /**
     * Removes a given Flow object from the list of active flows.
     * 
     * @param id unique identifier of the Flow object
     * @return true if operation was successful, or false if a problem occurred
     */
    @Override
    public boolean blockFlow(long id) {
        Flow flow;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
                return false;
            }
            flow = activeFlows.get(id);
            if (mappedFlows.containsKey(flow)) {
                return false;
            }
            activeFlows.remove(id);
            tr.blockFlow(flow);
            st.blockFlow(flow);
            return true;
        }
    }
    
    /**
     * Removes a given Flow object from the Physical Topology and then
     * puts it back, but with a new route (set of LightPath objects). 
     * 
     * @param id unique identifier of the Flow object
     * @param lightpaths list of LightPath objects, which form a Path
     * @return true if operation was successful, or false if a problem occurred
     */
    @Override
    public boolean rerouteFlow(long id, LightPath[] lightpaths) {
        Flow flow;
        Path oldPath;

        if (id < 0 || lightpaths.length < 1) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
                return false;
            }
            flow = activeFlows.get(id);
            if (!mappedFlows.containsKey(flow)) {
                return false;
            }
            oldPath = mappedFlows.get(flow);
            removeFlowFromPT(flow, lightpaths);
            if (!canAddFlowToPT(flow, lightpaths)) {
                addFlowToPT(flow, oldPath.getLightpaths());
                return false;
            }
            if(!checkLightpathContinuity(flow, lightpaths)){
                return false;
            }
            addFlowToPT(flow, lightpaths);
            mappedFlows.put(flow, new Path(lightpaths));
            //tr.flowRequest(id, true);
            return true;
        }
    }
    
    /**
     * Adds a given Flow object to the HashMap of active flows.
     * The HashMap also stores the object's unique identifier (ID). 
     * 
     * @param flow Flow object to be added
     */
    private void newFlow(Flow flow) {
        activeFlows.put(flow.getID(), flow);
    }
    
    /**
     * Removes a given Flow object from the list of active flows.
     * 
     * @param id the unique identifier of the Flow to be removed
     */
    private void removeFlow(long id) {
        Flow flow;
        LightPath[] lightpaths;
        if (activeFlows.containsKey(id)) {
            flow = activeFlows.get(id);
            if (mappedFlows.containsKey(flow)) {
                lightpaths = mappedFlows.get(flow).getLightpaths();
                removeFlowFromPT(flow, lightpaths);
                mappedFlows.remove(flow);
            }
            activeFlows.remove(id);
        }
    }
    
    /**
     * Removes a given Flow object from a Physical Topology. 
     * 
     * @param flow the Flow object that will be removed from the PT
     * @param lightpaths a list of LighPath objects
     */
    private void removeFlowFromPT(Flow flow, LightPath[] lightpaths) {
        for (LightPath lightpath : lightpaths) {
            int[] links; //doug
            links = lightpath.getLinks();
            for(int i=0;i<links.length;i++){
                pt.getLink(links[i]).removeUsage();
            }
            pt.removeFlow(flow, lightpath);
            
            // Can the lightpath be removed?
            if (vt.isLightpathIdle(lightpath.getID())) {
                vt.removeLightPath(lightpath.getID());
            }
        }
    }
    
    /**
     * Says whether or not a given Flow object can be added to a 
     * determined Physical Topology, based on the amount of bandwidth the
     * flow requires opposed to the available bandwidth.
     * 
     * @param flow the Flow object to be added 
     * @param lightpaths list of LightPath objects the flow uses
     * @return true if Flow object can be added to the PT, or false if it can't
     */
    private boolean canAddFlowToPT(Flow flow, LightPath[] lightpaths) {
        for (LightPath lightpath : lightpaths) {
            if (!pt.canAddFlow(flow, lightpath)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Adds a Flow object to a Physical Topology.
     * This means adding the flow to the network's traffic,
     * which simply decreases the available bandwidth.
     * 
     * @param flow the Flow object to be added 
     * @param lightpaths list of LightPath objects the flow uses
     */
    private void addFlowToPT(Flow flow, LightPath[] lightpaths) {
        for (LightPath lightpath : lightpaths) {
            pt.addFlow(flow, lightpath);
        }
    }
    
    /**Checks the lightpaths continuity in multihop and if flow src and dst is equal in lightpaths
     * 
     * @param flow the flow requisition
     * @param lightpaths the set of lightpaths
     * @return true if evething is ok, false otherwise
     */
    private boolean checkLightpathContinuity(Flow flow, LightPath[] lightpaths) {
        if(flow.getSource() == lightpaths[0].getSource() && flow.getDestination() == lightpaths[lightpaths.length-1].getDestination()){
            for (int i = 0; i < lightpaths.length - 1; i++) {
                if(!(lightpaths[i].getDestination() == lightpaths[i+1].getSource())){
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Retrieves a Path object, based on a given Flow object.
     * That's possible thanks to the HashMap mappedFlows, which
     * maps a Flow to a Path.
     * 
     * @param flow Flow object that will be used to find the Path object
     * @return Path object mapped to the given flow 
     */
    @Override
    public Path getPath(Flow flow) {
        return mappedFlows.get(flow);
    }
    
    /**
     * Retrieves the complete set of Flow/Path pairs listed on the
     * mappedFlows HashMap.
     * 
     * @return the mappedFlows HashMap
     */
    @Override
    public Map<Flow, Path> getMappedFlows() {
        return mappedFlows;
    }
    
    /**
     * Retrieves a Flow object from the list of active flows.
     * 
     * @param id the unique identifier of the Flow object
     * @return the required Flow object
     */
    @Override
    public Flow getFlow(long id) {
        return activeFlows.get(id);
    }
    
    /**
     * Counts number of times a given LightPath object
     * is used within the Flow objects of the network.
     * 
     * @param id unique identifier of the LightPath object
     * @return integer with the number of times the given LightPath object is used
     */
    @Override
    public int getLightpathFlowCount(long id) {
        int num = 0;
        Path p;
        LightPath[] lps;
        ArrayList<Path> ps = new ArrayList<>(mappedFlows.values());
        for (Path p1 : ps) {
            p = p1;
            lps = p.getLightpaths();
            for (LightPath lp : lps) {
                if (lp.getID() == id) {
                    num++;
                    break;
                }
            }
        }
        return num;
    }
    
    /**
     * Retrieves the PhysicalTopology object
     * @return PhysicalTopology object
     */
    @Override
    public PhysicalTopology getPT(){
        return pt;
    }
    
    /**
     * Retrieves the VirtualTopology object
     * @return VirtualTopology object
     */
    @Override
    public VirtualTopology getVT(){
        return vt;
    }
    
    /**
     * Creates a WDM LightPath candidate to put in the Virtual Topology (this method should be used by RA classes)
     * @param src the source node of the lightpath
     * @param dst the destination node of the lightpath
     * @param links the id links used by lightpath
     * @param wavelengths the wavelengths used by lightpath
     * @return the WDMLightPath object
     */
    @Override
    public WDMLightPath createCandidateWDMLightPath(int src, int dst, int[] links, int[] wavelengths) {
        return new WDMLightPath(1, src, dst, links, wavelengths);
    }

    /**
     * Creates a EON LightPath candidate to put in the Virtual Topology (this method should be used by RA classes)
     * @param src the source node of the lightpath
     * @param dst the destination node of the lightpath
     * @param links the id links used by lightpath
     * @param firstSlot the first slot used in this lightpath
     * @param lastSlot the last slot used in this lightpath
     * @param modulation the modulation id used in this lightpath
     * @return the EONLightPath object
     */
    @Override
    public EONLightPath createCandidateEONLightPath(int src, int dst, int[] links, int firstSlot, int lastSlot, int modulation) {
        return new EONLightPath(1, src, dst, links, firstSlot, lastSlot, modulation, EONPhysicalTopology.getSlotSize());
    }
}
