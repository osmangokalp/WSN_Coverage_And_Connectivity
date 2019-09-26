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
 * Imp. of Differential Evolution Algorithm at
 * https://en.wikipedia.org/wiki/Differential_evolution
 *
 * @author user
 */
public class DE extends Optimizer {

    //DE parameters
    private double F, CR; //F: differential weight, CR: crossover probability
    private int NP; //population size

    private double[][] solutions;
    private double[] fValues;

    private final Random rng;
    private final ObjectiveFunction objFunc;
    private final int dimension;
    private final int upperBound;
    private final int lowerBound;
    private double[] best;
    private double fBest;
    private final MainWindow mainWindow;

    public DE(double F, double CR, Random rng, Problem problem, MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.F = F;
        this.CR = CR;

        this.rng = rng;
        objFunc = problem.objFunc;
        dimension = problem.numOfSensors * 2;
        upperBound = problem.areaWidth;
        lowerBound = 0;
    }

    @Override
    public void solve(Population population, int startFEs, int endFEs) {
        NP = population.NP;
        solutions = population.solutions;
        fValues = population.fValues;
        best = population.solBest;
        fBest = population.fBest;

        int FEs = startFEs;

        while (FEs < endFEs) {
            if (FEs % 100 == 0) {
                mainWindow.FES_Changed(FEs);
            }
            for (int j = 0; j < NP; j++) { //For each agent x in the population
                double[] x = solutions[j];
                double fX = fValues[j];

                //Pick three agents a, b and c from the population at random, they must be distinct from each other as well as from agent
                int rA, rB, rC, rD;

                do {
                    rA = rng.nextInt(NP);
                } while (rA == j);

                double[] a = solutions[rA];

                do {
                    rB = rng.nextInt(NP);
                } while (rB == j || rB == rA);

                double[] b = solutions[rB];

                do {
                    rC = rng.nextInt(NP);
                } while (rC == j || rC == rA || rC == rB);
                
                double[] c = solutions[rC];
                
                do {
                    rD = rng.nextInt(NP);
                } while (rD == j || rD == rA || rD == rB || rD == rC);

                double[] d = solutions[rD];

                int R = rng.nextInt(dimension); //Pick a random dimension index R 

                double rI;
                double[] y = new double[dimension]; //the agent's potentially new position

                for (int i = 0; i < dimension; i++) {
                    rI = rng.nextDouble();
                    if (rI < CR || i == R) {
                        y[i] = a[i] + F * (b[i] - c[i]); //rand/1/bin
                        //y[i] = x[i] + F * (best[i] - x[i]) + F * (a[i] - b[i]); //rand-to-best/1/bin
                        //y[i] = x[i] + F * (best[i] - x[i]) + F * (a[i] - b[i]) + F * (c[i] - d[i]); //rand-to-best/2/bin
                    } else {
                        y[i] = x[i];
                    }
                }

                fixBoundary(y);

                double fY = objFunc.value(y);
                FEs++;
                if (fY > fX) {
                    //replace the agent in the population with the improved candidate solution, that is, replace x with y in the population
                    solutions[j] = y;
                    fValues[j] = fY;

                    if (fY > fBest) {
                        //replace the global best
                        System.arraycopy(y, 0, best, 0, dimension);
                        mainWindow.solutionChanged(best, fBest, objFunc.coverageRatio, objFunc.connected);
                        fBest = fY;
                        this.connected = objFunc.connected;
                        this.coverage = objFunc.coverageRatio;
                        //System.out.println("New best: " + fBest);
                        if (fBest == 2.0) { //best solution found
                            return;
                        }
                    }
                }

                if (FEs >= endFEs) { //if maximum FEs budget is reached then exit loop
                    break;
                }
            }

        }
        mainWindow.FES_Changed(FEs);
        population.solutions = solutions;
        population.fValues = fValues;
        population.solBest = best;
        population.fBest = fBest;

    }

    private void fixBoundary(double[] solution) {
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] > upperBound) {
                solution[i] = upperBound;
            } else if (solution[i] < lowerBound) {
                solution[i] = lowerBound;
            }
        }
    }
}
