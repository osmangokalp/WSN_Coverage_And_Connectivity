/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithm;

import gui.MainWindow;
import java.util.ArrayList;
import java.util.Random;
import model.Problem;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author user
 */
public class SaDE extends Optimizer {

    //DE parameters
    private double[] F;
    double[][] CR;

    private double[][] solutions;
    private double[] fValues;

    private int G; //generation counter
    private int NP; //population size
    private int LP; //learning period (between 20 and 60)
    private final int K = 4; //number of strategies 
    private double[] P; //probabilities of strategies
    private double[] CRm; //CR median values for the strategies
    private int[][] successMemory, failureMemory;
    private ArrayList<ArrayList<Double>> CRMemory;
    private final double epsilon = 0.01; //small constant value to avoid possible null success rates
    private double[] S; //score values for calculating probabilities
    MersenneTwister mt; //random generator for normal distributions
    private NormalDistribution FDist; //normal distribution generator for F values
    private final Random rng;
    private final ObjectiveFunction objFunc;
    private final int upperBound;
    private final int lowerBound;
    private final int dimension;
    private double[] solBest;
    private double fBest;
    private final MainWindow mainWindow;
    
    public SaDE(Random rng, Problem problem, MainWindow mainWindow) {
        this.rng = rng;
        this.mainWindow = mainWindow;
        objFunc = problem.objFunc;
        dimension = problem.numOfSensors * 2;
        upperBound = problem.areaWidth;
        lowerBound = 0;
    }

    private void init() {
        G = 0;
        LP = 50;

        P = new double[K];
        double initP = 1.0 / K;

        CRm = new double[K];
        double initCRm = 0.5;

        for (int k = 0; k < K; k++) {
            P[k] = initP;
            CRm[k] = initCRm;
        }

        successMemory = new int[LP][K];
        failureMemory = new int[LP][K];

        for (int i = 0; i < LP; i++) {
            for (int k = 0; k < K; k++) {
                successMemory[i][k] = 0;
                failureMemory[i][k] = 0;
            }
        }

        CRMemory = new ArrayList(K);
        for (int k = 0; k < K; k++) {
            CRMemory.add(new ArrayList<>());
        }

        S = new double[K];

        F = new double[NP];
        CR = new double[K][NP];

        mt = new MersenneTwister(2019);
        FDist = new NormalDistribution(mt, 0.5, 0.3);
    }

    @Override
    public void solve(Population population, int startFEs, int endFEs) {
        NP = population.NP;
        solutions = population.solutions;
        fValues = population.fValues;
        solBest = population.solBest;
        fBest = population.fBest;
        init();
        int FEs = startFEs;
        while (FEs < endFEs) {
            if (FEs % 100 == 0) {
                mainWindow.FES_Changed(FEs);
            }
            //Calculate strategy probability pk,G and update the success and failure memory
            if (G >= LP) {
                //step 3.1
                updateP(); //update the pk,G by equation (14)
                //upcoming nsk and nfk values are going to override success and failure memory later (so, there is no need for remove operation here)
            }
            //step 3.2
            /*assging trial vector generation strategy*/
            int[] selectedStrategies = selectStrategiesBySUS();
            /*assign control parameter F*/
            F = FDist.sample(NP);
            /*assging control parameter CR*/
            if (G >= LP) {
                for (int k = 0; k < K; k++) {
                    ArrayList<Double> dArr = CRMemory.get(k);
                    DescriptiveStatistics ds = new DescriptiveStatistics();
                    for (double d : dArr) {
                        ds.addValue(d);
                    }
                    CRm[k] = ds.getPercentile(50); //median
                }
            }
            for (int k = 0; k < K; k++) {
                NormalDistribution CRDist = new NormalDistribution(mt, CRm[k], 0.1);
                for (int i = 0; i < NP; i++) {
                    CR[k][i] = CRDist.sample();
                    while (CR[k][i] < 0 || CR[k][i] > 1) {
                        CR[k][i] = CRDist.sample();
                    }
                }
            }
            //step 3.3 Generate a new population
            double[][] U = new double[NP][dimension];
            for (int i = 0; i < NP; i++) {
                int k = selectedStrategies[i];
                U[i] = generateTrialVector(i, k, CR[k][i], F[i]);
            }
            //step 3.4 randomly reinitialize the trial vector Uik within the search space if any variable is outside its boundaries
            for (int i = 0; i < NP; i++) {
                for (int j = 0; j < dimension; j++) {
                    if (U[i][j] > upperBound || U[i][j] < lowerBound) {
                        U[i][j] = lowerBound + rng.nextDouble() * (upperBound - lowerBound);
                    }
                }
            }
            //step 3.5 selection
            int[] nS = new int[K];
            int[] nF = new int[K];
            for (int k = 0; k < K; k++) {
                nS[k] = 0;
                nF[k] = 0;
            }
            for (int i = 0; i < NP; i++) {
                if (FEs >= endFEs) {
                    //if maximum FEs budget is reached then exit loop
                    break; //interrupt employed bees
                }
                double fU = objFunc.value(U[i]);
                int k = selectedStrategies[i];
                FEs++;
                if (fU > fValues[i]) {
                    solutions[i] = U[i];
                    fValues[i] = fU;
                    nS[k] += 1;
                    CRMemory.get(k).add(CR[k][i]);
                    if (fU > fBest) {
                        //check if the best solution is improved
                        solBest = U[i].clone();
                        fBest = fU;
                        mainWindow.solutionChanged(solBest, fBest, objFunc.coverageRatio, objFunc.connected);
                        this.connected = objFunc.connected;
                        this.coverage = objFunc.coverageRatio;
                        //System.out.println("New best: " + fBest);
                        if (fBest == 2.0) {
                            //best solution found
                            return;
                        }
                    }
                } else {
                    nF[k] += 1;
                }
            }
            //delete old records from CRMemory (before updating the success memory below)
            for (int k = 0; k < K; k++) {
                if (G >= LP) {
                    int count = successMemory[G % LP][k];
                    for (int i = 0; i < count; i++) {
                        CRMemory.get(k).remove(0); //remove the oldest entries
                    }
                }
            }
            //store nSk and nFk (k=1,...,K) into the Success and Failure Memory
            for (int k = 0; k < K; k++) {
                successMemory[G % LP][k] = nS[k];
                failureMemory[G % LP][k] = nF[k];
            }
            //step 3.6
            G++; //increment the generation counter
        }
        mainWindow.FES_Changed(FEs);
        population.solutions = solutions;
        population.fValues = fValues;
        population.solBest = solBest;
        population.fBest = fBest;
    }

    private void updateP() {
        //calculate Sk,G
        double totalS = 0;
        int nskTotal = 0;
        int nfkTotal = 0;
        for (int k = 0; k < K; k++) {
            for (int i = 0; i < LP; i++) {
                nskTotal += successMemory[i][k];
                nfkTotal += failureMemory[i][k];
            }
            S[k] = (double) nskTotal / (double) (nskTotal + nfkTotal) + epsilon;
            totalS += S[k];
        }

        //calculate Pk,G
        for (int k = 0; k < K; k++) {
            P[k] = S[k] / totalS;
        }
    }

    //code taken from: http://puzzloq.blogspot.com.tr/2013/03/stochastic-universal-sampling.html
    //population - set of individuals' segments. Segment is equal to individual's fitness.
    //n - number of individuals to choose from population.
    private int[] selectStrategiesBySUS() {

        // Calculate total fitness of population
        double f = 0.0;
        for (double segment : P) {
            f += segment;
        }
        // Calculate distance between the pointers
        double p = f / NP;
        // Pick random number between 0 and p
        double start = rng.nextDouble() * p;
        // Pick n individuals
        int[] individuals = new int[NP];
        int index = 0;
        double sum = P[index];
        for (int i = 0; i < NP; i++) {
            // Determine pointer to a segment in the population
            double pointer = start + i * p;
            // Find segment, which corresponds to the pointer
            if (sum >= pointer) {
                individuals[i] = index;
            } else {
                for (++index; index < P.length; index++) {
                    sum += P[index];
                    if (sum >= pointer) {
                        individuals[i] = index;
                        break;
                    }
                }
            }
        }
        // Return the set of indexes, pointing to the chosen individuals
        return individuals;
    }

    private double[] generateTrialVector(int i, int strategy, double CR, double F) {
        double[] Xi = solutions[i];
        double[] Xr1, Xr2, Xr3, Xr4 = null, Xr5 = null;
        double[] Ui = new double[dimension];
        int r1, r2, r3, r4 = 0, r5, jRand;

        //select jRand
        jRand = rng.nextInt(dimension);

        //select r1
        r1 = i;
        while (r1 == i) {
            r1 = rng.nextInt(NP);
        }
        Xr1 = solutions[r1];

        //select r2
        r2 = i;
        while (r2 == i || r2 == r1) {
            r2 = rng.nextInt(NP);
        }
        Xr2 = solutions[r2];

        //select r3
        r3 = i;
        while (r3 == i || r3 == r1 || r3 == r2) {
            r3 = rng.nextInt(NP);
        }
        Xr3 = solutions[r3];

        if (strategy == 1 || strategy == 2) {
            //select r4
            r4 = i;
            while (r4 == i || r4 == r1 || r4 == r2 || r4 == r3) {
                r4 = rng.nextInt(NP);
            }
            Xr4 = solutions[r4];
        }

        if (strategy == 2) {
            //select r5
            r5 = i;
            while (r5 == i || r5 == r1 || r5 == r2 || r5 == r3 || r5 == r4) {
                r5 = rng.nextInt(NP);
            }
            Xr5 = solutions[r5];
        }

        switch (strategy) {
            case 0:
                /* DE/rand/1/bin */
                for (int j = 0; j < dimension; j++) {
                    if (rng.nextDouble() < CR || j == jRand) {
                        Ui[j] = Xr1[j] + F * (Xr2[j] - Xr3[j]);
                    } else {
                        Ui[j] = Xi[j];
                    }
                }
                break;
            case 1:
                /* DE/rand-to-best/2/bin */
                for (int j = 0; j < dimension; j++) {
                    if (rng.nextDouble() < CR || j == jRand) {
                        Ui[j] = Xi[j] + F * (solBest[j] - Xi[j]) + F * (Xr1[j] - Xr2[j]) + F * (Xr3[j] - Xr4[j]);
                    } else {
                        Ui[j] = Xi[j];
                    }
                }
                break;
            case 2:
                /* DE/rand/2/bin */
                for (int j = 0; j < dimension; j++) {
                    if (rng.nextDouble() < CR || j == jRand) {
                        Ui[j] = Xr1[j] + F * (Xr2[j] - Xr3[j]) + F * (Xr4[j] - Xr5[j]);
                    } else {
                        Ui[j] = Xi[j];
                    }
                }
                break;
            case 3:
                /* DE/current-to-rand/1 */
                for (int j = 0; j < dimension; j++) {
                    double paramK = rng.nextDouble();
                    Ui[j] = Xi[j] + paramK * (Xr1[j] - Xi[j]) + F * (Xr2[j] - Xr3[j]);
                }
                break;
        }

        return Ui;
    }

}
