/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithm;

import java.util.Random;

/**
 *
 * @author osman
 */
public class Util {

    public static double[] createRandomSensorCoordinates(Random rng, int dim, int upperBound, int lowerBound) {
        double[] arr = new double[dim];

        for (int i = 0; i < dim; i++) {
            arr[i] = lowerBound + rng.nextInt(upperBound - lowerBound);
        }
        return arr;
    }
    
    public static double[] createTargets(Random rng, int numOfTargets, int areaWidth, int targetDeploymentStrategy) {
        double[] targets = new double[numOfTargets * 2];
        
        int n = (int) Math.sqrt(numOfTargets);
        int interval = areaWidth / n;
        int gap = interval / 2;

        switch (targetDeploymentStrategy) {
            case 0:
                for (int i = 0; i < numOfTargets * 2; i += 2) {
                    targets[i] = rng.nextInt(areaWidth);
                    targets[i + 1] = rng.nextInt(areaWidth);
                }
                break;
            case 1:
                int counter = 0;
                int x = gap;
                for (int i = 0; i < n; i++) {
                    int y = gap;
                    for (int j = 0; j < n; j++) {
                        targets[counter++] = x;
                        targets[counter++] = y;
                        y += interval;
                    }
                    x += interval;
                }
                break;
            default:
                break;
        }
        
        return targets;
    }
    
    public static Population createRandomPopulation(Random rng, int NP, int dim, int upperBound, int lowerBound, ObjectiveFunction objFunc) {
                
        double[][] solutions = new double[NP][dim];
        double[] fValues = new double[NP];
        double[] solBest = new double[dim];
        double fBest = 1.0;
        for (int i = 0; i < NP; i++) {
            solutions[i] = Util.createRandomSensorCoordinates(rng, dim, upperBound, lowerBound);
            double f = objFunc.value(solutions[i]);
            fValues[i] = f;
            if (f <= fBest) {
                fBest = f;
                System.arraycopy(solutions[i], 0, solBest, 0, dim);
            }
        }
        
        Population pop = new Population();
        pop.NP = NP;
        pop.fBest = fBest;
        pop.fValues = fValues;
        pop.solBest = solBest;
        pop.solutions = solutions;
        
        return pop;
    }
    
}
