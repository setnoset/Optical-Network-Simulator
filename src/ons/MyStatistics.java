package ons;

/**
 * This class calculates and print all simulator statistics.
 * @author onsteam
 */
public class MyStatistics {

    private static MyStatistics singletonObject;
    private int minNumberArrivals;
    private int numberArrivals;
    private int arrivals;
    private int departures;
    private int accepted;
    private int blocked;
    private long requiredBandwidth;
    private long blockedBandwidth;
    private int numNodes;
    private int[][] arrivalsPairs;
    private int[][] blockedPairs;
    private long[][] requiredBandwidthPairs;
    private long[][] blockedBandwidthPairs;
    private int numfails;
    private int flowfails;
    private int lpsfails;
    private float trafficfails;
    private long execTime;
    //for Number of transmiters
    private long numLightPaths;
    private long numTransponders = 0;
    private int MAX_NumTransponders;
    private long usedTransponders = 0;
    //for available slots
    private long times = 0;
    private long availableSlots;
    private boolean firstTime = false;
    private int MAX_AvailableSlots;
    //for virtual hops per request
    private long virtualHops = 0;
    //for physical hops per request
    private long physicalHops = 0;
    //for modulations requests
    private long[] modulations;
    //for verbose counter
    private int verboseCount = 1;
    //Diffs
    private int numClasses;
    private int[] arrivalsDiff;
    private int[] blockedDiff;
    private long[] requiredBandwidthDiff;
    private long[] blockedBandwidthDiff;
    private int[][][] arrivalsPairsDiff;
    private int[][][] blockedPairsDiff;
    private int[][][] requiredBandwidthPairsDiff;
    private int[][][] blockedBandwidthPairsDiff;

    /**
     * A private constructor that prevents any other class from instantiating.
     */
    private MyStatistics() {

        numberArrivals = 0;

        arrivals = 0;
        departures = 0;
        accepted = 0;
        blocked = 0;

        requiredBandwidth = 0;
        blockedBandwidth = 0;

        numfails = 0;
        flowfails = 0;
        lpsfails = 0;
        trafficfails = 0;

        execTime = 0;
        //add by lucasrc
        numLightPaths = 0;
    }

    /**
     * Creates a new MyStatistics object, in case it does'n exist yet.
     *
     * @return the MyStatistics singletonObject
     */
    public static synchronized MyStatistics getMyStatisticsObject() {
        if (singletonObject == null) {
            singletonObject = new MyStatistics();
        }
        return singletonObject;
    }

    /**
     * Throws an exception to stop a cloned MyStatistics object from being
     * created.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Attributes initializer.
     *
     * @param pt PhysicalTopology Object
     * @param numClasses number of classes of service
     * @param minNumberArrivals minimum number of arriving events
     */
    public void statisticsSetup(PhysicalTopology pt, int numClasses, int minNumberArrivals) {
        this.numNodes = pt.getNumNodes();
        this.arrivalsPairs = new int[numNodes][numNodes];
        this.blockedPairs = new int[numNodes][numNodes];
        this.requiredBandwidthPairs = new long[numNodes][numNodes];
        this.blockedBandwidthPairs = new long[numNodes][numNodes];

        this.minNumberArrivals = minNumberArrivals;

        //Diff
        this.numClasses = numClasses;
        this.arrivalsDiff = new int[numClasses];
        this.blockedDiff = new int[numClasses];
        this.requiredBandwidthDiff = new long[numClasses];
        this.blockedBandwidthDiff = new long[numClasses];
        for (int i = 0; i < numClasses; i++) {
            this.arrivalsDiff[i] = 0;
            this.blockedDiff[i] = 0;
            this.requiredBandwidthDiff[i] = 0;
            this.blockedBandwidthDiff[i] = 0;
        }
        this.arrivalsPairsDiff = new int[numNodes][numNodes][numClasses];
        this.blockedPairsDiff = new int[numNodes][numNodes][numClasses];
        this.requiredBandwidthPairsDiff = new int[numNodes][numNodes][numClasses];
        this.blockedBandwidthPairsDiff = new int[numNodes][numNodes][numClasses];
        //
        if(pt instanceof EONPhysicalTopology) {
            this.modulations = new long[EONPhysicalTopology.getMaxModulation() + 1];
        } 
    }

    /**
     * Adds an accepted flow to the statistics.
     *
     * @param flow the accepted Flow object
     * @param lightpaths list of lightpaths in the flow
     */
    public void acceptFlow(Flow flow, LightPath[] lightpaths) {
        if (this.numberArrivals > this.minNumberArrivals) {
            this.accepted++;
            this.virtualHops(lightpaths.length);
            int count = 0;
            for (LightPath lps : lightpaths) {
                count += lps.getHops();
            }
            this.physicalHops(count);
        }
    }
    
    /**
     * Adds a blocked flow to the statistics.
     *
     * @param flow the blocked Flow object
     */
    public void blockFlow(Flow flow) {
        if (this.numberArrivals > this.minNumberArrivals) {
            int cos = flow.getCOS();
            this.blocked++;
            this.blockedDiff[cos]++;
            this.blockedBandwidth += flow.getRate();
            this.blockedBandwidthDiff[cos] += flow.getRate();
            this.blockedPairs[flow.getSource()][flow.getDestination()]++;
            this.blockedPairsDiff[flow.getSource()][flow.getDestination()][cos]++;
            this.blockedBandwidthPairs[flow.getSource()][flow.getDestination()] += flow.getRate();
            this.blockedBandwidthPairsDiff[flow.getSource()][flow.getDestination()][cos] += flow.getRate();
        }
    }
    
    /**
     * Adds an event to the statistics.
     * @param event the Event object to be added
     * @param availableSlots the atual available slots in physical topology
     * @param availableTransponders the atual available transponders in physical topology
     */
    public void addEvent(Event event, int availableSlots, int availableTransponders) {
        if(!firstTime){
            MAX_AvailableSlots = availableSlots;
            MAX_NumTransponders = availableTransponders;
            firstTime = true;
        }
        addEvent(event, availableTransponders);
        if (this.numberArrivals > this.minNumberArrivals) {
            this.availableSlots += (long) availableSlots;
        }
    }

    /**
     * Adds an event to the statistics.
     *
     * @param event the Event object to be added
     * @param availableTransponders the atual available transponders in physical topology
     */
    public void addEvent(Event event, int availableTransponders) {
        try {
            if (!firstTime) {
                MAX_NumTransponders = availableTransponders;
                firstTime = true;
            }
            times++;
            if (event instanceof FlowArrivalEvent) {
                this.numberArrivals++;
                if (this.numberArrivals > this.minNumberArrivals) {
                    this.numTransponders += (long) availableTransponders;
                    int cos = ((FlowArrivalEvent) event).getFlow().getCOS();
                    this.arrivals++;
                    this.arrivalsDiff[cos]++;
                    this.requiredBandwidth += ((FlowArrivalEvent) event).getFlow().getRate();
                    this.requiredBandwidthDiff[cos] += ((FlowArrivalEvent) event).getFlow().getRate();
                    this.arrivalsPairs[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()]++;
                    this.arrivalsPairsDiff[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()][cos]++;
                    this.requiredBandwidthPairs[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()] += ((FlowArrivalEvent) event).getFlow().getRate();
                    this.requiredBandwidthPairsDiff[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()][cos] += ((FlowArrivalEvent) event).getFlow().getRate();
                }
                //to print the current progress calls
                if (Simulator.verbose && (numberArrivals ==  10000*verboseCount)) {
                    System.out.println(Integer.toString(numberArrivals));
                    verboseCount++;
                }
            } else if (event instanceof FlowDepartureEvent) {
                if (this.numberArrivals > this.minNumberArrivals) {
                    this.departures++;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This function is called during the simulation execution, but only if
     * verbose was activated.
     *
     * @param simType 0 if the physicalTopology is WDM; 1 if physicalTopology is EON
     * @return string with the obtained statistics
     */
    public String fancyStatistics(int simType) {
        float acceptProb, blockProb, bbr, meanK;
        float bpDiff[], bbrDiff[];
        if (accepted == 0) {
            acceptProb = 0;
            meanK = 0;
        } else {
            acceptProb = ((float) accepted) / ((float) arrivals) * 100;
        }
        if (blocked == 0) {
            blockProb = 0;
            bbr = 0;
        } else {
            blockProb = ((float) blocked) / ((float) arrivals) * 100;
            bbr = ((float) blockedBandwidth) / ((float) requiredBandwidth) * 100;
        }
        bpDiff = new float[numClasses];
        bbrDiff = new float[numClasses];
        for (int i = 0; i < numClasses; i++) {
            if (blockedDiff[i] == 0) {
                bpDiff[i] = 0;
                bbrDiff[i] = 0;
            } else {
                bpDiff[i] = ((float) blockedDiff[i]) / ((float) arrivalsDiff[i]) * 100;
                bbrDiff[i] = ((float) blockedBandwidthDiff[i]) / ((float) requiredBandwidthDiff[i]) * 100;
            }
        }

        String stats = "Arrivals \t: " + Integer.toString(arrivals) + "\n";
        stats += "Departures \t: " + Integer.toString(departures) + "\n";
        stats += "Accepted \t: " + Integer.toString(accepted) + "\t(" + Float.toString(acceptProb) + "%)\n";
        stats += "Blocked \t: " + Integer.toString(blocked) + "\t(" + Float.toString(blockProb) + "%)\n";
        stats += "Required BW \t: " + Long.toString(requiredBandwidth) + "\n";
        stats += "Blocked BW \t: " + Long.toString(blockedBandwidth) + "\n";
        stats += "BBR: " + Float.toString(bbr) + "\n";
        stats += "Called Blocked by COS (%)" + "\n";
        for (int i = 0; i < numClasses; i++) {
            stats += "BP-" + Integer.toString(i) + " " + Float.toString(bpDiff[i]) + "%\n";
        }
        stats += "\nNumber of LPs: " + numLightPaths + "\n";
        double freeTransponders = (float) numTransponders/times; //free transponders/times-requests
        double freeTranspondersRatio = (float) ((freeTransponders*100.0)/MAX_NumTransponders);
        int nodes = blockedPairs[0].length;
        stats += "Average of free Transponders by node: " +(double) freeTransponders/nodes + " ("+MAX_NumTransponders/nodes+")\n";
        stats += "Available Transponders ratio: " + freeTranspondersRatio + "%\n";
        double used = (double) this.usedTransponders / (double) accepted;
        stats += "Average of Transponders per request: "+used+"\n";
        used = (double) this.virtualHops / (double) accepted;
        stats += "Average of Virtual Hops per request: "+used+"\n";
        used = (double) this.physicalHops / (double) accepted;
        stats += "Average of Physical Hops per request: "+used+"\n";
        
        if(simType == 1){
            double averageSpectrumAvailable = availableSlots/times;
            double spectrumAvailableRatio = (averageSpectrumAvailable*100.0)/MAX_AvailableSlots;
            stats += "Spectrum Available ratio: " + spectrumAvailableRatio + "%\n";
            for(int i = 0; i < modulations.length; i++){
                stats += Modulation.getModulationName(i) +" Modulation used: " + Float.toString((float) modulations[i]/(float) numLightPaths*100) + "%\n";
            }
        }
        stats += "\n";
        stats += "Blocking probability per s-d node-pair:\n";
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                if (i != j) {
                    stats += "Pair (" + Integer.toString(i) + "->" + Integer.toString(j) + ") ";
                    stats += "Calls (" + Integer.toString(arrivalsPairs[i][j]) + ")";
                    if (blockedPairs[i][j] == 0) {
                        blockProb = 0;
                        bbr = 0;
                    } else {
                        blockProb = ((float) blockedPairs[i][j]) / ((float) arrivalsPairs[i][j]) * 100;
                        bbr = ((float) blockedBandwidthPairs[i][j]) / ((float) requiredBandwidthPairs[i][j]) * 100;
                    }
                    stats += "\tBP (" + Float.toString(blockProb) + "%)";
                    stats += "\tBBR (" + Float.toString(bbr) + "%)\n";
                }
            }
        }
        return stats;
    }
    
    /**
     * Prints all the obtained statistics, but only if verbose was not
     * activated.
     * @param simType 0 if the physicalTopology is WDM; 1 if physicalTopology is EON
     */
    public void printStatistics(int simType) {
        float acceptProb, blockProb, bbr, meanK;
        float bpDiff[], bbrDiff[];
        if (accepted == 0) {
            acceptProb = 0;
            meanK = 0;
        } else {
            acceptProb = ((float) accepted) / ((float) arrivals) * 100;
        }
        if (blocked == 0) {
            blockProb = 0;
            bbr = 0;
        } else {
            blockProb = ((float) blocked) / ((float) arrivals) * 100;
            bbr = ((float) blockedBandwidth) / ((float) requiredBandwidth) * 100;
        }
        bpDiff = new float[numClasses];
        bbrDiff = new float[numClasses];
        for (int i = 0; i < numClasses; i++) {
            if (blockedDiff[i] == 0) {
                bpDiff[i] = 0;
                bbrDiff[i] = 0;
            } else {
                bpDiff[i] = ((float) blockedDiff[i]) / ((float) arrivalsDiff[i]) * 100;
                bbrDiff[i] = ((float) blockedBandwidthDiff[i]) / ((float) requiredBandwidthDiff[i]) * 100;
            }
        }

        String stats = "";
        stats += "BR \t: " + Float.toString(blockProb) + "%\n";
        stats += "BBR: " + Float.toString(bbr) + "\n";
        stats += "Called Blocked by COS (%)" + "\n";
        for (int i = 0; i < numClasses; i++) {
            stats += "BP-" + Integer.toString(i) + " " + Float.toString(bpDiff[i]) + "%\n";
        }
        stats += "\nLPs: " + numLightPaths + "\n";
        double freeTransponders = (float) numTransponders/times; //free transponders/times-requests
        double freeTranspondersRatio = (float) ((freeTransponders*100.0)/MAX_NumTransponders);     
        stats += "Available Transponders: " + freeTranspondersRatio + "%\n";
        double used = (double) this.usedTransponders / (double) accepted;
        stats += "Transponders per request: "+used+"\n";
        used = (double) this.virtualHops / (double) accepted;
        stats += "Virtual Hops per request: "+used+"\n";
        used = (double) this.physicalHops / (double) accepted;
        stats += "Physical Hops per request: "+used+"\n";
        
        if(simType == 1){
            double averageSpectrumAvailable = availableSlots/times;
            double spectrumAvailableRatio = (averageSpectrumAvailable*100.0)/MAX_AvailableSlots;
            stats += "Spectrum Available: " + spectrumAvailableRatio + "%\n";
            for(int i = 0; i < modulations.length; i++){
                stats += Modulation.getModulationName(i) +" Modulation used: " + Float.toString((float) modulations[i]/(float) numLightPaths*100) + "%\n";
            }
        }
        System.out.println(stats);
    }

    /**
     * When a lightpath is allocated
     * @param lp the lightpath
     */
    public void createLightpath(LightPath lp) {
        this.numLightPaths++;
        if (lp instanceof EONLightPath) {
            this.modulations[((EONLightPath) lp).getModulation()]++;
        }
    }
    
    /**
     * When a lightpath is deallocated
     * @param lp the lightpath
     */
    public void deallocatedLightpath(LightPath lp) {
        this.numLightPaths--;
        if (lp instanceof EONLightPath) {
            this.modulations[((EONLightPath) lp).getModulation()]--;
        }
    }
    
    /**
     * When a transponder is allocated
     * @param usedTransponders 
     */
    public void userTransponder(int usedTransponders) {
        this.usedTransponders += (long) usedTransponders;
    }
    
    /**
     * When a new virtual hop occurs
     * @param virtualHops 
     */
    public void virtualHops(int virtualHops) {
        this.virtualHops += (long) virtualHops;
    }
    
    /**
     * When a new physical hop occurs
     * @param physicalHops 
     */
    public void physicalHops(int physicalHops) {
        this.physicalHops += (long) physicalHops;
    }
    
    /**
     * Terminates the singleton object.
     */
    public void finish() {
        singletonObject = null;
    }
}
