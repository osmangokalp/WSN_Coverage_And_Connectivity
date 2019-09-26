/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithm;

import gui.MainWindow;
import java.util.Random;
import model.Problem;

/**
 *
 * @author osman
 */
public class ABC extends Optimizer {

    //ABC control parameters
    private int NP; //Colony size
    private int foodNumber; //The number of food sources
    private final int limit;

    private double[][] solutions;
    private double[] fValues;
    private int[] trial;
    private final Random rng;
    private final ObjectiveFunction objFunc;
    private final int dimension;
    private final int upperBound;
    private final int lowerBound;
    private double[] solBest;
    private double fBest;
    private final MainWindow mainWindow;

    public ABC(int limit, Random rng, Problem problem, MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.limit = limit;
        this.rng = rng;

        objFunc = problem.objFunc;
        dimension = problem.numOfSensors * 2;
        upperBound = problem.areaWidth;
        lowerBound = 0;
    }

    @Override
    public void solve(Population population, int startFEs, int endFEs) {
        solutions = population.solutions;
        fValues = population.fValues;
        solBest = population.solBest;
        fBest = population.fBest;
        foodNumber = solutions.length;
        NP = foodNumber * 2;
        trial = new int[foodNumber];

        int FEs = startFEs, param2change, neighborIndex;
        double[] curSol, neighborSol, newSol;
        double newF;

        while (FEs < endFEs) {
            if (FEs % 100 == 0) {
                mainWindow.FES_Changed(FEs);
            }
            /*Employed Bee Phase*/
            for (int i = 0; i < foodNumber; i++) {
                if (FEs >= endFEs) { //if maximum FEs budget is reached then exit loop
                    break; //interrupt employed bees
                }

                param2change = rng.nextInt(dimension);
                do {
                    neighborIndex = rng.nextInt(foodNumber);
                } while (neighborIndex == i); //neighborIndex must be different from i

                curSol = solutions[i];
                neighborSol = solutions[neighborIndex];
                newSol = new double[dimension];
                System.arraycopy(curSol, 0, newSol, 0, dimension);
                newSol[param2change] = curSol[param2change] + (curSol[param2change] - neighborSol[param2change]) * (rng.nextDouble() - 0.5) * 2;

                fixBoundary(newSol);

                newF = objFunc.value(newSol);
                FEs++;

                if (newF > fValues[i]) { //if the mutant solution is better than the original
                    trial[i] = 0; //reset trial number
                    solutions[i] = newSol;
                    fValues[i] = newF;
                } else {
                    trial[i]++; //increase the trial number of ith food source
                }

            }

            //calculate probabilities
            double maxfit;
            double[] prob = new double[foodNumber];
            maxfit = fValues[0];

            for (int i = 1; i < foodNumber; i++) {
                if (fValues[i] > maxfit) {
                    maxfit = fValues[i];
                }
            }

            for (int i = 0; i < foodNumber; i++) {
                prob[i] = (0.9 * (fValues[i] / maxfit)) + 0.1;
            }

            //send onlooker bees
            int i = 0;
            int t = 0;

            while (t < NP - foodNumber) {
                if (FEs >= endFEs) { //if maximum FEs budget is reached then exit loop
                    break; //interrupt onlooker bees
                }
                double r = rng.nextDouble();

                if (r < prob[i]) {
                    t++;

                    param2change = rng.nextInt(dimension);

                    do {
                        neighborIndex = rng.nextInt(foodNumber);
                    } while (neighborIndex == i); //neighborIndex must be different from i

                    curSol = solutions[i];
                    neighborSol = solutions[neighborIndex];
                    newSol = new double[dimension];
                    System.arraycopy(curSol, 0, newSol, 0, dimension);
                    newSol[param2change] = curSol[param2change] + (curSol[param2change] - neighborSol[param2change]) * (rng.nextDouble() - 0.5) * 2;

                    fixBoundary(newSol);

                    newF = objFunc.value(newSol);
                    FEs++;

                    if (newF > fValues[i]) { //if the mutant solution is better than the original
                        trial[i] = 0; //reset trial number
                        solutions[i] = newSol;
                        fValues[i] = newF;

                    } else {
                        trial[i]++; //increase the trial number of ith food source
                    }

                }
                i++;
                if (i == this.foodNumber) {
                    i = 0;
                }
            }

            //update global best
            for (i = 0; i < foodNumber; i++) {
                if (fValues[i] > fBest) {
                    objFunc.value(solutions[i]);
                    System.arraycopy(solutions[i], 0, solBest, 0, dimension);
                    fBest = fValues[i];
                    mainWindow.solutionChanged(solBest, fBest, objFunc.coverageRatio, objFunc.connected);
                    this.connected = objFunc.connected;
                    this.coverage = objFunc.coverageRatio;
                    if (fBest == 2.0) {
                        //best solution found
                        return;
                    }
                }
            }

            if (FEs >= endFEs) { //if maximum FEs budget is reached then exit loop
                break; //do not send scout bees
            }

            //send scout bees
            int maxtrialindex = 0;
            for (int k = 1; k < foodNumber; k++) {
                if (this.trial[k] > trial[maxtrialindex]) {
                    maxtrialindex = k;
                }
            }

            if (trial[maxtrialindex] >= limit) {
                double[] randomSol = new double[dimension];
                for (int k = 0; k < dimension; k++) { //create a random solution within the specified initialization bounds
                    randomSol[k] = lowerBound + rng.nextDouble() * (upperBound - lowerBound);
                }

                solutions[maxtrialindex] = randomSol;
                fValues[maxtrialindex] = objFunc.value(randomSol);
                FEs++;
                trial[maxtrialindex] = 0; //init the trial number
            }

        }
        mainWindow.FES_Changed(FEs);
        //update the population with the latest status
        population.solutions = solutions;
        population.fValues = fValues;
        population.solBest = solBest;
        population.fBest = fBest;

    }

    public void fixBoundary(double[] solution) {
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] > upperBound) {
                solution[i] = upperBound;
            } else if (solution[i] < lowerBound) {
                solution[i] = lowerBound;
            }
        }
    }

}
