/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ons;

import java.io.File;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Centralizes the simulation execution. Defines what the command line
 * arguments do, and extracts the simulation information from the XML file.
 * 
 * @author onsteam
 */
public class Simulator {

    private static String simName;
    private static final Float simVersion = (float) 1.0;
    public static boolean verbose = false;
    public static boolean trace = false;
    
    /**
     * Executes simulation based on the given XML file and the used command line arguments.
     * 
     * @param simConfigFile name of the XML file that contains all information about the simulation
     * @param trace activates the Tracer class functionalities
     * @param verbose activates the printing of information about the simulation, on runtime, for debugging purposes
     * @param forcedLoad range of loads for which several simulations are automated; if not specified, load is taken from the XML file
     * @param seed a number in the interval [1,25] that defines up to 25 different random simulations
     */
    public void Execute(String simConfigFile, boolean trace, boolean verbose, double forcedLoad, int seed) {

        Simulator.verbose = verbose;
        Simulator.trace = trace;

        if (Simulator.verbose) {
            System.out.println("########################################################");
            System.out.println("# ONS - Optical Network Simulator - version " + simVersion.toString() + "  #by Douglas");
            System.out.println("#######################################################\n");
        }

        try {

            long begin = System.currentTimeMillis();

            if (Simulator.verbose) {
                System.out.println("(0) Accessing simulation file " + simConfigFile + "...");
            }
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(simConfigFile));

            // normalize text representation
            doc.getDocumentElement().normalize();

            // check the root TAG name and version
            int simType = -1;
            simName = doc.getDocumentElement().getNodeName();  
            switch (simName) {
                case "wdmsim":
                    if(Simulator.verbose)
                        System.out.println("Simulation type: " + simName + " (WDM)");
                    simType = 0;
                    break;
                case "eonsim":
                    if(Simulator.verbose)
                        System.out.println("Simulation type: " + simName + " (EON)");
                    simType = 1;
                    break;
                default:
                    System.out.println("Root element of the simulation file is " + doc.getDocumentElement().getNodeName() + ", eonsim or wdmsim is expected!");
                    System.exit(0);
            }
            
            if (!doc.getDocumentElement().hasAttribute("version")) {
                System.out.println("Cannot find version attribute!");
                System.exit(0);
            }
            if (Float.compare(new Float(doc.getDocumentElement().getAttribute("version")), simVersion) > 0) {
                System.out.println("Simulation config file requires a newer version of the simulator!");
                System.exit(0);
            }
            if (Simulator.verbose) {
                System.out.println("(0) Done. (" + Float.toString((float) ((float) (System.currentTimeMillis() - begin) / (float) 1000)) + " sec)\n");
            }

            /*
             * Extract physical topology part
             */
            begin = System.currentTimeMillis();
            if (Simulator.verbose) {
                System.out.println("(1) Loading physical topology information...");
            }
            
            PhysicalTopology pt;
            if (simType == 0){
            	pt = new WDMPhysicalTopology((Element) doc.getElementsByTagName("physical-topology").item(0));
            } else{
            	pt = new EONPhysicalTopology((Element) doc.getElementsByTagName("physical-topology").item(0));
            }
            
            
            
            if (Simulator.verbose) {
                System.out.println(pt);
            }

            if (Simulator.verbose) {
                System.out.println("(1) Done. (" + Float.toString((float) ((float) (System.currentTimeMillis() - begin) / (float) 1000)) + " sec)\n");
            }


            /*
             * Extract virtual topology part
             */
            begin = System.currentTimeMillis();
            if (Simulator.verbose) {
                System.out.println("(2) Loading virtual topology information...");
            }

            VirtualTopology vt = new VirtualTopology((Element) doc.getElementsByTagName("virtual-topology").item(0), pt);
            		
            if (Simulator.verbose) {
                System.out.println(vt);
            }

            if (Simulator.verbose) {
                System.out.println("(2) Done. (" + Float.toString((float) ((float) (System.currentTimeMillis() - begin) / (float) 1000)) + " sec)\n");
            }

            /*
             * Extract simulation traffic part
             */
            begin = System.currentTimeMillis();
            if (Simulator.verbose) {
                System.out.println("(3) Loading traffic information...");
            }

            EventScheduler events = new EventScheduler();
            TrafficGenerator traffic = new TrafficGenerator((Element) doc.getElementsByTagName("traffic").item(0), forcedLoad);
            traffic.generateTraffic(pt, events, seed);

            if (Simulator.verbose) {
                System.out.println("(3) Done. (" + Float.toString((float) ((float) (System.currentTimeMillis() - begin) / (float) 1000)) + " sec)\n");
            }

            /*
             * Extract simulation setup part
             */
            begin = System.currentTimeMillis();
            if (Simulator.verbose) {
                System.out.println("(4) Loading simulation setup information...");
            }
            
            /*
             * Extract PMD OSNR delta
             */
            pt.setOSNR(Double.parseDouble(((Element) doc.getElementsByTagName("traffic").item(0)).getAttribute("OSNR")));
            pt.setPMD(Double.parseDouble(((Element) doc.getElementsByTagName("traffic").item(0)).getAttribute("PMD")));
            pt.setdelta(Double.parseDouble(((Element) doc.getElementsByTagName("traffic").item(0)).getAttribute("delta")));
            pt.setexponencial(Integer.parseInt(((Element) doc.getElementsByTagName("traffic").item(0)).getAttribute("exponencial")));
            
            MyStatistics st = MyStatistics.getMyStatisticsObject();
            int numberOfCOS = 0;
            if(((Element) doc.getElementsByTagName("traffic").item(0)).hasAttribute("cos")){
                numberOfCOS = Integer.parseInt(((Element) doc.getElementsByTagName("traffic").item(0)).getAttribute("cos"));
                if(numberOfCOS == 0){
                    throw (new IllegalArgumentException("\"cos\" in xml can not be \"0\""));
                }
            } else {
                throw (new IllegalArgumentException("\"cos\" in xml was not set"));
            }
            int statisticStart = 0;
            if(((Element) doc.getElementsByTagName("traffic").item(0)).hasAttribute("statisticStart")){
                statisticStart = Integer.parseInt(((Element) doc.getElementsByTagName("traffic").item(0)).getAttribute("statisticStart"));
            }
            st.statisticsSetup(pt, numberOfCOS, statisticStart);
            
            Tracer tr = Tracer.getTracerObject();
            if (Simulator.trace == true)
            {
            	if (forcedLoad == 0) {
                	tr.setTraceFile(simConfigFile.substring(0, simConfigFile.length() - 4) + ".trace");
            	} else {
                	tr.setTraceFile(simConfigFile.substring(0, simConfigFile.length() - 4) + "_Load_" + Double.toString(forcedLoad) + ".trace");
            	}
            }
            tr.toogleTraceWriting(Simulator.trace);
            
            String raModule = "ons.ra." + ((Element) doc.getElementsByTagName("ra").item(0)).getAttribute("module");
            if (Simulator.verbose) {
                System.out.println("RA module: " + raModule);
            }
            ControlPlane cp = new ControlPlane(raModule, pt, vt);

            if (Simulator.verbose) {
                System.out.println("(4) Done. (" + Float.toString((float) ((float) (System.currentTimeMillis() - begin) / (float) 1000)) + " sec)\n");
            }

            /*
             * Run the simulation
             */
            begin = System.currentTimeMillis();
            if (Simulator.verbose) {
                System.out.println("(5) Running the simulation...");
            }

            SimulationRunner sim = new SimulationRunner(cp, events);

            if (Simulator.verbose) {
                System.out.println("(5) Done. (" + Float.toString((float) ((float) (System.currentTimeMillis() - begin) / (float) 1000)) + " sec)\n");
            }

            if (Simulator.verbose) {
                if (forcedLoad == 0) {
                    System.out.println("Statistics (" + simConfigFile + "):\n");
                } else {
                    System.out.println("Statistics for " + Double.toString(forcedLoad) + " erlangs (" + simConfigFile + "):\n");
                }
                System.out.println(st.fancyStatistics(simType));
            } else {
                System.out.println("*****");
                if (forcedLoad != 0) {
                    System.out.println("Load:" + Double.toString(forcedLoad));
                }
                st.printStatistics(simType);
            }
            
            // Terminate MyStatistics singleton
            st.finish();

            // Flush and close the trace file and terminate the singleton
            if (Simulator.trace == true)
            	tr.finish();
            
        } catch (SAXParseException err) {
            System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
            System.out.println(" " + err.getMessage());

        } catch (SAXException e) {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
  
