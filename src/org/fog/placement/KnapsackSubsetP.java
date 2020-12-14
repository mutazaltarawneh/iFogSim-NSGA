package org.fog.placement;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.util.Vector;
import org.moeaframework.util.io.CommentedLineReader;

import org.moeaframework.problem.AbstractProblem;
/**
 * Multiobjective 0/1 knapsack problem. Problem instances are loaded from files
 * in the format defined by Eckart Zitzler and Marco Laumanns at <a href=
 * "http://www.tik.ee.ethz.ch/sop/download/supplementary/testProblemSuite/">
 * http://www.tik.ee.ethz.ch/sop/download/supplementary/testProblemSuite/</a>.
 */


//public class Knapsack implements Problem {
public class KnapsackSubsetP extends AbstractProblem {
	/**
	 * The number of sacks.
	 */
	protected static int nsacks = 2;

	/**
	 * The number of items.
	 */
	protected int nitems;

	/**
	 * Entry {@code profit[i][j]} is the profit from including item {@code j}
	 * in sack {@code i}.
	 */
	protected double[][] profit;

	/**
	 * Entry {@code weight[i][j]} is the weight incurred from including item
	 * {@code j} in sack {@code i}.
	 */
	protected double[][] weight;

	/**
	 * Entry {@code capacity[i]} is the weight capacity of sack {@code i}.
	 */
	protected int[] capacity;

	/**
	 * Constructs a multiobjective 0/1 knapsack problem instance loaded from
	 * the specified file.
	 * 
	 * @param file the file containing the knapsack problem instance
	 * @throws IOException if an I/O error occurred
	 */
	
	public KnapsackSubsetP(double [][] profit, double [][] weight, int nitems, int capa ) {
		super(1, nsacks, nsacks);
		this.profit = profit;
		this.weight = weight;
		this.nitems = nitems;
		
		capacity = new int[nsacks];
		
		for (int i=0;i<nsacks;i++)
		{
			this.capacity[i] = capa;
		}
	}
	
	
	public void evaluate(Solution solution) {
		
		
		int[] items = EncodingUtils.getSubset(solution.getVariable(0));
		double[] f = new double[nsacks];
		double[] g = new double[nsacks];

		// calculate the profits and weights for the knapsacks
		for (int i = 0; i < items.length; i++) {
				for (int j = 0; j < nsacks; j++) {
					f[j] += profit[j][i];
					g[j] += weight[j][i];
				}
			
		}

		// check if any weights exceed the capacities
		for (int j = 0; j < nsacks; j++) {
			if (g[j] <= capacity[j]) {
				g[j] = 0.0;
			} else {
				g[j] = g[j] - capacity[j];
			}
		}

		// negate the objectives since Knapsack is maximization
		solution.setObjectives(Vector.negate(f));
		solution.setConstraints(g);
	}
	
	@Override
	public String getName() {
		return "KnapsackP";
	}

	@Override
	public int getNumberOfConstraints() {
		return nsacks;
	}

	@Override
	public int getNumberOfObjectives() {
		return nsacks;
	}

	@Override
	public int getNumberOfVariables() {
		return 1;
	}

	@Override
	public Solution newSolution() {
		Solution solution = new Solution(1, nsacks, nsacks);
		solution.setVariable(0, EncodingUtils.newSubset(0, nitems, nitems));
		return solution;
	}

	@Override
	public void close() {
		//do nothing
	}
}