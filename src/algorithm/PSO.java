/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithm;

import gui.MainWindow;
import java.util.Random;
import model.Problem;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;

/**
 *
 * @author osman
 */
public class PSO extends Optimizer {

    //PSO params
    private int NP;
    private final double w, c1, c2;
    private int K = 3;

    private NormalDistribution nDist; //for generating random points in hyperspere
    private Random rng;
    private double[] best;
    private double fBest;
    private final ObjectiveFunction objFunc;
    private final int dimension;
    private final int upperBound;
    private final int lowerBound;
    private final MainWindow mainWindow;

    public PSO(Random rng, Problem problem, MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.w = 1.0 / (2.0 * Math.log(2.0));
        this.c1 = 0.5 + Math.log(2.0);
        this.c2 = 0.5 + Math.log(2.0);
        this.rng = rng;

        objFunc = problem.objFunc;
        dimension = problem.numOfSensors * 2;
        upperBound = problem.areaWidth;
        lowerBound = 0;
        MersenneTwister mt = new MersenneTwister(rng.nextInt(100));
        nDist = new NormalDistribution(mt, 0, 1);
    }

    @Override
    public void solve(Population population, int startFEs, int endFEs) {
        NP = population.NP;

        //get initial positions, obj func values and global best
        double[][] X = population.solutions;
        double[] f = population.fValues;
        best = population.solBest;
        fBest = population.fBest;

        //create V, pBest, lBest and relevant vectors
        double[][] V = new double[NP][dimension]; //velocity vector
        double[][] P = new double[NP][dimension]; //personal best vector
        double[] fP = new double[NP]; //obj func values of P vector 

        int[][] neighborhoodMatrix = new int[NP][NP]; //neighborhood matrix

        //initialize V and pBest vectors
        for (int i = 0; i < NP; i++) {
            System.arraycopy(X[i], 0, P[i], 0, dimension);
            fP[i] = f[i];

            //random velocity
            for (int j = 0; j < dimension; j++) {
                V[i][j] = randBetweenInterval(lowerBound - X[i][j], upperBound - X[i][j]);
            }
        }

        int FEs = startFEs;
        boolean globalBestImproved = false;

        while (FEs < endFEs) {
            if (FEs % 100 == 0) {
                mainWindow.FES_Changed(FEs);
            }
            if (globalBestImproved == false) { //create random topology
                //reset neighborhood matrix 
                for (int i = 0; i < NP; i++) {
                    for (int j = 0; j < NP; j++) {
                        neighborhoodMatrix[i][j] = 0;
                    }
                }

                // construct random topology
                for (int i = 0; i < NP; i++) {
                    neighborhoodMatrix[i][i] = 1; //every particle informs itself
                    for (int k = 0; k < K; k++) {
                        int randNeighbor = rng.nextInt(NP);
                        neighborhoodMatrix[i][randNeighbor] = 1;
                    }
                }
            }

            globalBestImproved = false; //reset before the new cycle

            //start new cycle
            for (int i = 0; i < NP; i++) {

                double p, l, G, U1, U2;

                //find best neighbor
                int L = i; //set best neighbor as itself
                for (int j = 0; j < NP; j++) {
                    if (neighborhoodMatrix[i][j] == 1) { //if j is the neighbor of i
                        if (f[j] > f[L]) {
                            L = j;
                        }
                    }
                }

                double[] points = samplePointsInHyperSpehe();

                // update dimensions individually
                for (int j = 0; j < dimension; j++) {
                    U1 = rng.nextDouble();
                    U2 = rng.nextDouble();
                    p = X[i][j] + c1 * U1 * (P[i][j] - X[i][j]);
                    l = X[i][j] + c2 * U2 * (X[L][j] - X[i][j]);
                    if (L == i) { //if the best informant is the particle itself
                        G = (X[i][j] + p) / 2.0;
                    } else {
                        G = (X[i][j] + p + l) / 3.0;
                    }

                    double radius = G - X[i][j];
                    double xPrime = radius * points[j] + G;
                    V[i][j] = w * V[i][j] + xPrime - X[i][j];
                    X[i][j] = X[i][j] + V[i][j];

                    //check boundary conditions
                    if (X[i][j] < lowerBound) {
                        X[i][j] = lowerBound;
                        V[i][j] = -0.5 * V[i][j];
                    } else if (X[i][j] > upperBound) {
                        X[i][j] = upperBound;
                        V[i][j] = -0.5 * V[i][j];
                    }
                }

                //calculate new fitness
                f[i] = objFunc.value(X[i]);
                FEs++;
                if (f[i] > fP[i]) { //better then its previous best
                    System.arraycopy(X[i], 0, P[i], 0, dimension);
                    fP[i] = f[i];
                    if (f[i] > fBest) {
                        System.arraycopy(X[i], 0, best, 0, dimension);
                        fBest = f[i];
                        globalBestImproved = true;
                        mainWindow.solutionChanged(best, fBest, objFunc.coverageRatio, objFunc.connected);
                        this.connected = objFunc.connected;
                        this.coverage = objFunc.coverageRatio;
                        if (fBest == 2.0) {
                            //best solution found
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
        population.solutions = X;
        population.fValues = f;
        population.solBest = best;
        population.fBest = fBest;

    }

    private double[] samplePointsInHyperSpehe() {
        double[] points = nDist.sample(dimension);

        double length = 0;
        for (int i = 0; i < dimension; i++) {
            length += points[i] * points[i];
        }

        length = Math.sqrt(length);

        double r = rng.nextDouble(); //uniform dist

        for (int i = 0; i < dimension; i++) {
            points[i] = r * points[i] / length;
        }

        return points;
    }

    private double randBetweenInterval(double a, double b) {
        return a + rng.nextDouble() * (b - a);
    }
}
