/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import algorithm.ObjectiveFunction;
import algorithm.Util;
import java.util.Random;

/**
 *
 * @author osman
 */
public class Problem {

    public Random rng;
    public int areaWidth;
    public int numOfSensors;
    public int numOfTargets;
    public int sensingRadius;
    public int transmissionRadius;
    public int K;
    public double[] targetCoordinates;
    public ObjectiveFunction objFunc;

    int targetDeploymentStrategy; //0: random, 1: equally distributed

    public Problem(int areaWidth, int numOfSensors, int numOfTargets, int sensingRadius, int transmissionRadius, int K, int targetDeploymentStrategy, Random rng) {
        this.areaWidth = areaWidth;
        this.numOfSensors = numOfSensors;
        this.numOfTargets = numOfTargets;
        this.sensingRadius = sensingRadius;
        this.transmissionRadius = transmissionRadius;
        this.K = K;
        this.targetDeploymentStrategy = targetDeploymentStrategy;
        this.rng = rng;

        init();
    }

    private void init() {

        targetCoordinates = Util.createTargets(rng, numOfTargets, areaWidth, targetDeploymentStrategy);

        objFunc = new ObjectiveFunction(this, K);

    }


}
