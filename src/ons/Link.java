package ons;


/**
 * The abstract class Link
 * @author onsteam
 */

public abstract class Link {
	
    protected int id;
    protected int src;
    protected int dst;
    protected double delay;
    protected double weight;
    protected int usage = 1;
    protected double amps = 0;

    /**
     * Creates a new Fiberlink object.
     * 
     * @param id            unique identifier
     * @param src           source node
     * @param dst           destination node
     * @param delay         propagation delay (miliseconds)
     * @param weight        optional link weight
     */
    public Link(int id, int src, int dst, double delay, double weight) {
        if (id < 0 || src < 0 || dst < 0) {
            throw (new IllegalArgumentException());
        } else {
            this.id = id;
            this.src = src;
            this.dst = dst;
            this.delay = delay;
            this.weight = weight;
            this.amps = weight/80;
            //System.out.println("ID: "+this.id+" AMPS: "+this.amps);
        }
    }
    
    /**
     *  doug a fazer
     * 
     * @return the value of usage
     */
    public int getUsage(){
        return this.usage;
    }
    
    /**
     *  doug a fazer
     *
     * @param usage
     */
    public void setUsage(int usage){
        this.usage = usage;
    }
    
    public double getAmps(){
        return this.amps;
    }
   
    /**
     *  doug a fazer
     *
     */
    public void addUsage(){
        this.usage++;
    }
    
    /**
     *  doug a fazer
     *
     */
    public void removeUsage(){
        this.usage--;
    }
    
    /**
     * Retrieves the unique identifier for a given Link.
     * 
     * @return the value of the Link's id attribute
     */
    public int getID() {
        return this.id;
    }
    
    /**
     * Retrieves the source node for a given Link.
     * 
     * @return the value of the Link's src attribute
     */
    public int getSource() {
        return this.src;
    }
    
    /**
     * Retrieves the destination node for a given WDMLink.
     * 
     * @return the value of the Link's dst attribute
     */
    public int getDestination() {
        return this.dst;
    }
    
    /**
     * Retrieves the weight for a given Link.
     * 
     * @return the value of the Link's weight attribute
     */
    public double getWeight() {
        return this.weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    /**
     * Retrieves the propagation delay for a given Link.
     * 
     * @return the value of the WDMLink's delay attribute
     */
    public double getDelay() {
        return this.delay;
    }
    
    /**
     * Prints all information related to the Link object.
     * 
     * @return string containing all the values of the link's parameters.
     */
    @Override
    public abstract String toString();

}
