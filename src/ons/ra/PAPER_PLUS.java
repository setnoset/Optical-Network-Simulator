/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ons.ra;

import static java.lang.Math.log10;
import static java.lang.Math.sqrt;
import ons.Flow;
import ons.LightPath;
import ons.WDMLightPath;
import ons.WDMPhysicalTopology;
import ons.util.Dijkstra;
import ons.util.WeightedGraph;

public class PAPER_PLUS implements RA {
    
    private ControlPlaneForRA cp;
    private WeightedGraph graph;
    private WeightedGraph graph2;


    @Override
    public void simulationInterface(ControlPlaneForRA cp) {
        this.cp = cp;
        this.graph = cp.getPT().getWeightedGraph();
    }

    @Override
    public void flowArrival(Flow flow) {
        double rate = flow.getRate()/1000;
        int[] nodes;
        int[] links;
        int[] wvls;
        long id;
        LightPath[] lps = new LightPath[1];
        
        this.graph2 = cp.getPT().getUsageGraph();
        // Shortest-Path routing
        nodes = Dijkstra.getShortestPath(graph2, flow.getSource(), flow.getDestination());
        // If no possible path found, block the call
        if (nodes.length == 0) {
            cp.blockFlow(flow.getID()); 
            return;
        }
        // Create the links vector
        links = new int[nodes.length - 1];
        for (int j = 0; j < nodes.length - 1; j++) {
            links[j] = cp.getPT().getLink(nodes[j], nodes[j + 1]).getID();
        }
        if (checkPhysicalLink(links,rate) == false) {
            cp.blockFlow(flow.getID()); 
            return;
        }
        // First-Fit wavelength assignment
        wvls = new int[links.length];
        for (int i = 0; i < ((WDMPhysicalTopology) cp.getPT()).getNumWavelengths(); i++) {
            // Create the wavelengths vector
            for (int j = 0; j < links.length; j++) {
                wvls[j] = i;
            }
            // Now you create the lightpath to use the createLightpath VT
            WDMLightPath lp = new WDMLightPath(1, flow.getSource(), flow.getDestination(), links, wvls);
            // Now you try to establish the new lightpath, accept the call
            if ((id = cp.getVT().createLightpath(lp)) >= 0) {
                // Single-hop routing (end-to-end lightpath)
                lps[0] = cp.getVT().getLightpath(id);
                if (cp.acceptFlow(flow.getID(), lps)) {
                    for(int l=0; l<(nodes.length-1);l++){
                        cp.getPT().getLink(nodes[l], nodes[l+1]).addUsage();
                    }
                    return;
                } else {
                    // Something wrong
                    // Dealocates the lightpath in VT and try again
                    cp.getVT().deallocatedLightpath(id);
                }
                // Block the call
                
            }
        }
        cp.blockFlow(flow.getID());
    }

    @Override
    public void flowDeparture(long id) {
        // Do anything before flow departure
    }

    @Override
    public boolean checkPhysicalLink(int[] Links, double rate) {
        //        final double pmd = 7e-14;
//        final double delta = 2.5e-12; // 10.0p p 10g 2.5 p 40g 1.0 p 100g
        double temp = 0,bw = rate*10e+8,result;
        boolean check = true;
        
        /*
        teste de OSNR
        */
        double F = 3.1623, F2 = 3.9810, Pin = 0.001, G = 158.4893,G2 = 158.4893, h = 6.626069e-34, v = 1.934e+14 , Bo = 12.5e+9, m = 0, n = 0, Nase1=0, Nase2=0, Nin=0, OSNRo=0,Nout=0;
        
        if(cp.getPT().getOSNR()!=0){
            for(int j=0;j<Links.length;j++){
                if(cp.getPT().getLink(Links[j]).getWeight()<300){
                    m = m +cp.getPT().getLink(Links[j]).getAmps();
                }
                else{
                    n = n +cp.getPT().getLink(Links[j]).getAmps();
                }
            }
            Nase1 = F * G * h * v * Bo;
            Nase2 = F2 * G2 * h * v * Bo;
            Nin = Pin / cp.getPT().getOSNR() ;
            Nout = Nin + m*Nase1 + n*Nase2;
            OSNRo = Pin/Nout;
            if(rate==10){
                if(10*log10(OSNRo)<13){ //21 17
                    check=false;
                    return check;
                }
            }
            else{
                if(10*log10(OSNRo)<19){
                    check=false;
                    return check;
                }
            }
        }
        /*
        teste de PMD
        */
        if(cp.getPT().getPMD()!=0){
            for(int i=0;i<Links.length;i++){
                temp = temp + cp.getPT().getLink(Links[i]).getWeight();
            }
            result = cp.getPT().getPMD()*sqrt(temp);
            if(result>cp.getPT().getdelta()){
                check = false;
            }
        }
        return check;
    }   
}
