/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithm;

/**
 *
 * @author osman
 */
public abstract class Optimizer {

    public boolean connected;
    public double coverage;

    public abstract void solve(Population population, int startFEs, int endFEs);

}
