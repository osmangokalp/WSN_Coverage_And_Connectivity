/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithm;

import model.Problem;

/**
 *
 * @author osman
 */
public class ObjectiveFunction {

    Problem problem;
    int[] coverageCounts;
    int[][] connectivityMatrix;
    public boolean connected;
    public double coverageRatio;
    public int connectedComponentCount;

    public ObjectiveFunction(Problem problem, int K) {
        this.problem = problem;
        coverageCounts = new int[problem.numOfTargets];
        connectivityMatrix = new int[problem.numOfSensors][problem.numOfSensors];
    }

    public double value(double[] arr) {
        resetCoverageCounts();

        for (int i = 0; i < problem.numOfSensors; i++) {
            double x1 = arr[i * 2];
            double y1 = arr[i * 2 + 1];
            for (int j = 0; j < problem.numOfTargets; j++) {
                double x2 = problem.targetCoordinates[j * 2];
                double y2 = problem.targetCoordinates[j * 2 + 1];

                double dist = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
                if (dist <= problem.sensingRadius) {
                    coverageCounts[j]++;
                }
            }
        }

        int numOfCovered = 0;
        for (int i = 0; i < coverageCounts.length; i++) {
            if (coverageCounts[i] >= problem.K) {
                numOfCovered++;
            }
        }

        coverageRatio = (double) numOfCovered / (double) problem.numOfTargets;
        checkConnectedness(arr);

        double W1 = 0.0, W2 = 1.0;
        if (connected) {
            W1 = 1.0;
        }         

        /*if (coverageRatio == 1) {
            return (connected ? 1 : 0);
        }
        
        if (connected) {
            return coverageRatio;
        }*/
        double objValue = W1 * coverageRatio + W2 * (1.0 / connectedComponentCount);

        /*if (objValue == 1.0) {
            System.out.println("");
        }*/
        return objValue;

    }

    private void checkConnectedness(double[] arr) {
        for (int i = 0; i < problem.numOfSensors; i++) {
            double x1 = arr[i * 2];
            double y1 = arr[i * 2 + 1];
            for (int j = 0; j < problem.numOfSensors; j++) {
                double x2 = arr[j * 2];
                double y2 = arr[j * 2 + 1];

                if (i != j) {
                    connectivityMatrix[i][j] = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) <= problem.transmissionRadius ? 1 : 0;
                } else {
                    connectivityMatrix[i][j] = 0;
                }
            }
        }

        boolean[] visited = new boolean[problem.numOfSensors];
        for (int i = 0; i < visited.length; i++) {
            visited[i] = false;
        }

        int visitedCount = 0;
        connectedComponentCount = 0;

        while (visitedCount != problem.numOfSensors) {
            int unvisitedIndex = -1;
            for (int i = 0; i < visited.length; i++) {
                if (!visited[i]) {
                    unvisitedIndex = i;
                }
            }
            
            DFS(unvisitedIndex, visited);
            connectedComponentCount++;
            
            visitedCount = 0;
            for (int i = 0; i < visited.length; i++) {
                if (visited[i]) {
                    visitedCount++;
                }
            }
        }

        connected = (connectedComponentCount == 1);
    }

    private void resetCoverageCounts() {
        for (int i = 0; i < coverageCounts.length; i++) {
            coverageCounts[i] = 0;
        }
    }

    private void DFS(int v, boolean[] visited) {
        // Mark the current node as visited and print it 
        visited[v] = true;
        // Recur for all the vertices 
        // adjacent to this vertex 
        for (int i = 0; i < problem.numOfSensors; i++) {
            if (connectivityMatrix[v][i] == 1) {
                if (!visited[i]) {
                    DFS(i, visited);
                }
            }
        }

    }
}
