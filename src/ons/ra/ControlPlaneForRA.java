/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ons.ra;

import ons.EONLightPath;
import ons.LightPath;
import ons.Flow;
import ons.Path;
import ons.PhysicalTopology;
import ons.VirtualTopology;
import ons.WDMLightPath;
import java.util.Map;

/**
 * This is the interface that provides several methods for the
 * RWA Class within the Control Plane.
 * 
 * @author onsteam
 */
public interface ControlPlaneForRA {

    public boolean acceptFlow(long id, LightPath[] lightpaths);

    public boolean blockFlow(long id);

    public boolean rerouteFlow(long id, LightPath[] lightpaths);
    
    public Flow getFlow(long id);
    
    public Path getPath(Flow flow);
    
    public int getLightpathFlowCount(long id);

    public Map<Flow, Path> getMappedFlows();

    public PhysicalTopology getPT();
    
    public VirtualTopology getVT();
    
    public WDMLightPath createCandidateWDMLightPath(int src, int dst, int[] links, int[] wavelengths);
    
    public EONLightPath createCandidateEONLightPath(int src, int dst, int[] links, int firstSlot, int lastSlot, int modulation);
}
