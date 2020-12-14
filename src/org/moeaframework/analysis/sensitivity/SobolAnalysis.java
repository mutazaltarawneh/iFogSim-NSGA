/* Copyright 2009-2018 David Hadka
 *
 * This file is part of the MOEA Framework.
 *
 * The MOEA Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * The MOEA Framework is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the MOEA Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.moeaframework.analysis.sensitivity;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.stat.StatUtils;
import org.moeaframework.core.PRNG;
import org.moeaframework.util.CommandLineUtility;

/**
 * Global sensitivity analysis of blackbox model output using Saltelli's
 * improved Sobol' global variance decomposition procedure.
 * <p>
 * The following code was derived and translated from the C code used in the
 * study cited below. Refer to this article for a description of the procedure.
 * <p>
 * References:
 * <ol>
 * <li>Tang, Y., Reed, P., Wagener, T., and van Werkhoven, K., "Comparing
 * Sensitivity Analysis Methods to Advance Lumped Watershed Model Identification
 * and Evaluation," Hydrology and Earth System Sciences, vol. 11, no. 2, pp.
 * 793-817, 2007.
 * <li>Saltelli, A., et al. "Global Sensitivity Analysis: The Primer." John
 * Wiley & Sons Ltd, 2008.
 * </ol>
 */
public class SobolAnalysis extends CommandLineUtility {

	/**
	 * Number of resamples used to bootstrap the 50% confidence intervals.
	 */
	private int resamples = 1000;

	/**
	 * Parameters being analyzed.
	 */
	private ParameterFile parameterFile;

	/**
	 * Number of parameters.
	 */
	private int P;

	/**
	 * Number of samples.
	 */
	private int N;

	/**
	 * Index of the metric being evaluated.
	 */
	private int index;

	/**
	 * Output from the original parameters.
	 */
	private double[] A;

	/**
	 * Output from the resampled parameters.
	 */
	private double[] B;

	/**
	 * Output from the original samples where the j-th parameter is replaced by
	 * the corresponding resampled parameter.
	 */
	private double[][] C_A;

	/**
	 * Output from the resampled samples where the j-th parameter is replaced by
	 * the corresponding original parameter.
	 */
	private double[][] C_B;

	/**
	 * Constructs the command line utility for global sensitivity analysis
	 * using Sobol's global variance decomposition based on Saltelli's work.
	 */
	public SobolAnalysis() {
		super();
	}

	/**
	 * Loads the outputs from the file. Each line in the file must contain the
	 * output produced using the parameters generated by SobolSequence.
	 * 
	 * @param file the model output file
	 * @throws IOException if an I/O error occurred
	 */
	private void load(File file) throws IOException {
		MatrixReader reader = null;
		
		try {
			reader = new MatrixReader(file);

			A = new double[N];
			B = new double[N];
			C_A = new double[N][P];
			C_B = new double[N][P];

			for (int i = 0; i < N; i++) {
				A[i] = reader.next()[index];

				for (int j = 0; j < P; j++) {
					C_A[i][j] = reader.next()[index];
				}

				for (int j = 0; j < P; j++) {
					C_B[i][j] = reader.next()[index];
				}

				B[i] = reader.next()[index];
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * Computes and displays the first-, total-, and second- order Sobol'
	 * sensitivities and 50% bootstrap confidence intervals.
	 * 
	 * @param output the output stream
	 */
	private void display(PrintStream output) {
		output.println("Parameter	Sensitivity [Confidence]");

		output.println("First-Order Effects");
		for (int j = 0; j < P; j++) {
			double[] a0 = new double[N];
			double[] a1 = new double[N];
			double[] a2 = new double[N];

			for (int i = 0; i < N; i++) {
				a0[i] = A[i];
				a1[i] = C_A[i][j];
				a2[i] = B[i];
			}

			output.print("  ");
			output.print(parameterFile.get(j).getName());
			output.print(' ');
			output.print(computeFirstOrder(a0, a1, a2, N));
			output.print(" [");
			output.print(computeFirstOrderConfidence(a0, a1, a2, N, resamples));
			output.println(']');
		}

		output.println("Total-Order Effects");
		for (int j = 0; j < P; j++) {
			double[] a0 = new double[N];
			double[] a1 = new double[N];
			double[] a2 = new double[N];

			for (int i = 0; i < N; i++) {
				a0[i] = A[i];
				a1[i] = C_A[i][j];
				a2[i] = B[i];
			}

			output.print("  ");
			output.print(parameterFile.get(j).getName());
			output.print(' ');
			output.print(computeTotalOrder(a0, a1, a2, N));
			output.print(" [");
			output.print(computeTotalOrderConfidence(a0, a1, a2, N, resamples));
			output.println(']');
		}

		output.println("Second-Order Effects");
		for (int j = 0; j < P; j++) {
			for (int k = j + 1; k < P; k++) {
				double[] a0 = new double[N];
				double[] a1 = new double[N];
				double[] a2 = new double[N];
				double[] a3 = new double[N];
				double[] a4 = new double[N];

				for (int i = 0; i < N; i++) {
					a0[i] = A[i];
					a1[i] = C_B[i][j];
					a2[i] = C_A[i][k];
					a3[i] = C_A[i][j];
					a4[i] = B[i];
				}

				output.print("  ");
				output.print(parameterFile.get(j).getName());
				output.print(" * ");
				output.print(parameterFile.get(k).getName());
				output.print(' ');
				output.print(computeSecondOrder(a0, a1, a2, a3, a4, N));
				output.print(" [");
				output.print(computeSecondOrderConfidence(a0, a1, a2, a3, a4, N,
								resamples));
				output.println(']');
			}
		}
	}

	/**
	 * Computes and displays the first- and total-order Sobol' sensitivites and
	 * 50% bootstrap confidence intervals.
	 * 
	 * @param output the output stream
	 */
	private void displaySimple(PrintStream output) {
		output.println("First-Order Effects");
		for (int j = 0; j < P; j++) {
			double[] a0 = new double[N];
			double[] a1 = new double[N];
			double[] a2 = new double[N];

			for (int i = 0; i < N; i++) {
				a0[i] = A[i];
				a1[i] = C_A[i][j];
				a2[i] = B[i];
			}

			double value = computeFirstOrder(a0, a1, a2, N);
			output.print(value < 0 ? 0.0 : value);

			if (j < P - 1) {
				output.print('\t');
			}
		}

		output.println();
		output.println("Total-Order Effects");
		for (int j = 0; j < P; j++) {
			double[] a0 = new double[N];
			double[] a1 = new double[N];
			double[] a2 = new double[N];

			for (int i = 0; i < N; i++) {
				a0[i] = A[i];
				a1[i] = C_A[i][j];
				a2[i] = B[i];
			}

			double value = computeTotalOrder(a0, a1, a2, N);
			output.print(value < 0 ? 0.0 : value);

			if (j < P - 1) {
				output.print('\t');
			}
		}

		output.println();
	}

	/**
	 * Returns the first-order confidence interval of the i-th parameter.  The
	 * arguments to this method mirror the arguments to
	 * {@link #computeFirstOrder}.
	 * 
	 * @param a0 the output from the first independent samples
	 * @param a1 the output from the samples produced by swapping the i-th
	 *        parameter in the first independent samples with the i-th parameter
	 *        from the second independent samples
	 * @param a2 the output from the second independent samples
	 * @param nsample the number of samples
	 * @param nresample the number of resamples used when calculating the
	 *        confidence interval
	 * @return the first-order confidence interval of the i-th parameter
	 */
	private static double computeFirstOrderConfidence(double[] a0, double[] a1,
			double[] a2, int nsample, int nresample) {
		double[] b0 = new double[nsample];
		double[] b1 = new double[nsample];
		double[] b2 = new double[nsample];
		double[] s = new double[nresample];

		for (int i = 0; i < nresample; i++) {
			for (int j = 0; j < nsample; j++) {
				int index = PRNG.nextInt(nsample);

				b0[j] = a0[index];
				b1[j] = a1[index];
				b2[j] = a2[index];
			}

			s[i] = computeFirstOrder(b0, b1, b2, nsample);
		}

		double ss = StatUtils.sum(s) / nresample;
		double sss = 0.0;
		
		for (int i = 0; i < nresample; i++) {
			sss += Math.pow(s[i] - ss, 2.0);
		}

		return 1.96 * Math.sqrt(sss / (nresample - 1));
	}

	/**
	 * Returns the first-order sensitivity of the i-th parameter.  Note how
	 * the contents of the array {@code a1} specify the parameter being
	 * analyzed.
	 * 
	 * @param a0 the output from the first independent samples
	 * @param a1 the output from the samples produced by swapping the i-th
	 *        parameter in the first independent samples with the i-th parameter
	 *        from the second independent samples
	 * @param a2 the output from the second independent samples
	 * @param nsample the number of samples
	 * @return the first-order sensitivity of the i-th parameter
	 */
	private static double computeFirstOrder(double[] a0, double[] a1,
			double[] a2, int nsample) {
		double c = 0.0;
		for (int i = 0; i < nsample; i++) {
			c += a0[i];
		}
		c /= nsample;

		double tmp1 = 0.0;
		double tmp2 = 0.0;
		double tmp3 = 0.0;
		double EY2 = 0.0;

		for (int i = 0; i < nsample; i++) {
			EY2 += (a0[i] - c) * (a2[i] - c);
			tmp1 += (a2[i] - c) * (a2[i] - c);
			tmp2 += (a2[i] - c);
			tmp3 += (a1[i] - c) * (a2[i] - c);
		}

		EY2 /= nsample;

		double V = (tmp1 / (nsample - 1)) - Math.pow(tmp2 / nsample, 2.0);
		double U = tmp3 / (nsample - 1);

		return (U - EY2) / V;
	}

	/**
	 * Returns the total-order sensitivity of the i-th parameter.  Note how
	 * the contents of the array {@code a1} specify the parameter being
	 * analyzed.
	 * 
	 * @param a0 the output from the first independent samples
	 * @param a1 the output from the samples produced by swapping the i-th
	 *        parameter in the first independent samples with the i-th parameter
	 *        from the second independent samples
	 * @param a2 the output from the second independent samples
	 * @param nsample the number of samples
	 * @return the total-order sensitivity of the i-th parameter
	 */
	private static double computeTotalOrder(double[] a0, double[] a1,
			double[] a2, int nsample) {
		double c = 0.0;
		
		for (int i = 0; i < nsample; i++) {
			c += a0[i];
		}
		
		c /= nsample;

		double tmp1 = 0.0;
		double tmp2 = 0.0;
		double tmp3 = 0.0;

		for (int i = 0; i < nsample; i++) {
			tmp1 += (a0[i] - c) * (a0[i] - c);
			tmp2 += (a0[i] - c) * (a1[i] - c);
			tmp3 += (a0[i] - c);
		}

		double EY2 = Math.pow(tmp3 / nsample, 2.0);
		double V = (tmp1 / (nsample - 1)) - EY2;
		double U = tmp2 / (nsample - 1);

		return 1.0 - ((U - EY2) / V);
	}

	/**
	 * Returns the total-order confidence interval of the i-th parameter.  The
	 * arguments to this method mirror the arguments to
	 * {@link #computeTotalOrder}.
	 * 
	 * @param a0 the output from the first independent samples
	 * @param a1 the output from the samples produced by swapping the i-th
	 *        parameter in the first independent samples with the i-th parameter
	 *        from the second independent samples
	 * @param a2 the output from the second independent samples
	 * @param nsample the number of samples
	 * @param nresample the number of resamples used when calculating the
	 *        confidence interval
	 * @return the total-order confidence interval of the i-th parameter
	 */
	private static double computeTotalOrderConfidence(double[] a0, double[] a1,
			double[] a2, int nsample, int nresample) {
		double[] b0 = new double[nsample];
		double[] b1 = new double[nsample];
		double[] b2 = new double[nsample];
		double[] s = new double[nresample];

		for (int i = 0; i < nresample; i++) {
			for (int j = 0; j < nsample; j++) {
				int index = PRNG.nextInt(nsample);

				b0[j] = a0[index];
				b1[j] = a1[index];
				b2[j] = a2[index];
			}

			s[i] = computeTotalOrder(b0, b1, b2, nsample);
		}

		double ss = StatUtils.sum(s) / nresample;
		double sss = 0.0;
		
		for (int i = 0; i < nresample; i++) {
			sss += Math.pow(s[i] - ss, 2.0);
		}

		return 1.96 * Math.sqrt(sss / (nresample - 1));
	}

	/**
	 * Returns the second-order sensitivity of the i-th and j-th parameters.  
	 * Note how the contents of the arrays {@code a1}, {@code a2}, and
	 * {@code a3} specify the two parameters being analyzed.
	 * 
	 * @param a0 the output from the first independent samples
	 * @param a1 the output from the samples produced by swapping the i-th
	 *        parameter in the second independent samples with the i-th
	 *        parameter from the first independent samples
	 * @param a2 the output from the samples produced by swapping the j-th
	 *        parameter in the first independent samples with the j-th parameter
	 *        from the second independent samples
	 * @param a3 the output from the samples produced by swapping the i-th
	 *        parameter in the first independent samples with the i-th parameter
	 *        from the second independent samples
	 * @param a4 the output from the second independent samples
	 * @param nsample the number of samples
	 * @param nresample the number of resamples used when calculating the
	 *        confidence interval
	 * @return the second-order sensitivity of the i-th and j-th parameters
	 */
	private static double computeSecondOrder(double[] a0, double[] a1,
			double[] a2, double[] a3, double[] a4, int nsample) {
		double c = 0.0;
		
		for (int i = 0; i < nsample; i++) {
			c += a0[i];
		}
		
		c /= nsample;

		double EY = 0.0;
		double EY2 = 0.0;
		double tmp1 = 0.0;
		double tmp2 = 0.0;
		double tmp3 = 0.0;
		double tmp4 = 0.0;
		double tmp5 = 0.0;

		for (int i = 0; i < nsample; i++) {
			EY += (a0[i] - c) * (a4[i] - c);
			EY2 += (a1[i] - c) * (a3[i] - c);
			tmp1 += (a1[i] - c) * (a1[i] - c);
			tmp2 += (a1[i] - c);
			tmp3 += (a1[i] - c) * (a2[i] - c);
			tmp4 += (a2[i] - c) * (a4[i] - c);
			tmp5 += (a3[i] - c) * (a4[i] - c);
		}

		EY /= nsample;
		EY2 /= nsample;

		double V = (tmp1 / (nsample - 1)) - Math.pow(tmp2 / nsample, 2.0);
		double Vij = (tmp3 / (nsample - 1)) - EY2;
		double Vi = (tmp4 / (nsample - 1)) - EY;
		double Vj = (tmp5 / (nsample - 1)) - EY2;

		return (Vij - Vi - Vj) / V;
	}

	/**
	 * Returns the second-order confidence interval of the i-th and j-th
	 * parameters.  The arguments to this method mirror the arguments to
	 * {@link #computeSecondOrder}.
	 * 
	 * @param a0 the output from the first independent samples
	 * @param a1 the output from the samples produced by swapping the i-th
	 *        parameter in the second independent samples with the i-th
	 *        parameter from the first independent samples
	 * @param a2 the output from the samples produced by swapping the j-th
	 *        parameter in the first independent samples with the j-th parameter
	 *        from the second independent samples
	 * @param a3 the output from the samples produced by swapping the i-th
	 *        parameter in the first independent samples with the i-th parameter
	 *        from the second independent samples
	 * @param a4 the output from the second independent samples
	 * @param nsample the number of samples
	 * @return the second-order confidence interval of the i-th and j-th
	 *         parameters
	 */
	private static double computeSecondOrderConfidence(double[] a0,
			double[] a1, double[] a2, double[] a3, double[] a4, int nsample,
			int nresample) {
		double[] b0 = new double[nsample];
		double[] b1 = new double[nsample];
		double[] b2 = new double[nsample];
		double[] b3 = new double[nsample];
		double[] b4 = new double[nsample];
		double[] s = new double[nresample];

		for (int i = 0; i < nresample; i++) {
			for (int j = 0; j < nsample; j++) {
				int index = PRNG.nextInt(nsample);

				b0[j] = a0[index];
				b1[j] = a1[index];
				b2[j] = a2[index];
				b3[j] = a3[index];
				b4[j] = a4[index];
			}

			s[i] = computeSecondOrder(b0, b1, b2, b3, b4, nsample);
		}

		double ss = StatUtils.sum(s) / nresample;
		double sss = 0.0;
		
		for (int i = 0; i < nresample; i++) {
			sss += Math.pow(s[i] - ss, 2.0);
		}

		return 1.96 * Math.sqrt(sss / (nresample - 1));
	}

	/**
	 * Ensures the model output file contains N*(2P+2) lines and returns N, the
	 * number of samples.
	 * 
	 * @param file the model output file being validated
	 * @return the number of samples
	 * @throws IOException
	 */
	private int validate(File file) throws IOException {
		MatrixReader reader = null;
		
		try {
			reader = new MatrixReader(file);
			int count = 0;

			while (reader.hasNext()) {
				if (reader.next().length > index) {
					count++;
				} else {
					break;
				}
			}

			if (count % (2 * P + 2) != 0) {
				System.err.println(file + " is incomplete");
			}

			return count / (2 * P + 2);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@SuppressWarnings("static-access")
	@Override
	public Options getOptions() {
		Options options = super.getOptions();

		options.addOption(OptionBuilder
				.withLongOpt("parameterFile")
				.hasArg()
				.withArgName("file")
				.isRequired()
				.create('p'));
		options.addOption(OptionBuilder
				.withLongOpt("input")
				.hasArg()
				.withArgName("file")
				.isRequired()
				.create('i'));
		options.addOption(OptionBuilder
				.withLongOpt("metric")
				.hasArg()
				.withArgName("value")
				.isRequired()
				.create('m'));
		options.addOption(OptionBuilder
				.withLongOpt("simple")
				.create('s'));
		options.addOption(OptionBuilder
				.withLongOpt("output")
				.hasArg()
				.withArgName("file")
				.create('o'));
		options.addOption(OptionBuilder
				.withLongOpt("resamples")
				.hasArg()
				.withArgName("number")
				.create('r'));

		return options;
	}

	@Override
	public void run(CommandLine commandLine) throws Exception {
		PrintStream output = null;
		
		//setup the parameters
		parameterFile = new ParameterFile(new File(
				commandLine.getOptionValue("parameterFile")));
		index = Integer.parseInt(commandLine.getOptionValue("metric"));
		P = parameterFile.size();
		
		if (commandLine.hasOption("resamples")) {
			resamples = Integer.parseInt(commandLine.getOptionValue(
					"resamples"));
		}

		//load and validate the model output file
		File input = new File(commandLine.getOptionValue("input"));
		N = validate(input);
		load(input);

		try {
			//setup the output stream
			if (commandLine.hasOption("output")) {
				output = new PrintStream(new File(
						commandLine.getOptionValue("output")));
			} else {
				output = System.out;
			}
			
			//perform the Sobol analysis and display the results
			if (commandLine.hasOption("simple")) {
				displaySimple(output);
			} else {
				display(output);
			}
		} finally {
			if ((output != null) && (output != System.out)) {
				output.close();
			}
		}
	}

	/**
	 * Command line utility for global sensitivity analysis using Sobol's global
	 * variance decomposition based on Saltelli's work.
	 * 
	 * @param args the command line arguments
	 * @throws Exception if an error occurred
	 */
	public static void main(String[] args) throws Exception {
		new SobolAnalysis().start(args);
	}

}
