package org.uma.jmetal.algorithm.multiobjective.mombi;

import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Abstract class representing variants of the MOMBI algorithm
 * @author Juan J. Durillo
 * Modified by Antonio J. Nebro
 *
 * @param <S>
 */

public abstract class AbstractMOMBI<S extends Solution<?>> extends AbstractGeneticAlgorithm<S,List<S>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Problem<S> problem;
	private final int maxIterations;

	private int iterations = 0;
	private final SolutionListEvaluator<S> evaluator;
	private final List<Double> utopiaPoint;
	private final List<Double> nadirPoint;

	/**
	 * Constructor
	 *
	 * @param problem Problem to be solved
	 * @param maxIterations Maximum number of iterations the algorithm
	 * will perform
	 * @param crossover Crossover operator
	 * @param mutation Mutation operator
	 * @param selection Selection operator
	 * @param evaluator Evaluator object for evaluating solution lists
	 */
	public AbstractMOMBI(Problem<S> problem, int maxIterations,
											 CrossoverOperator<S> crossover, MutationOperator<S> mutation,
											 SelectionOperator<List<S>,S> selection,
											 SolutionListEvaluator<S> evaluator) {
		super();
		this.problem = problem;
		this.maxIterations = maxIterations;

		this.crossoverOperator 	= crossover;
		this.mutationOperator  	= mutation;
		this.selectionOperator  = selection;

		this.evaluator = evaluator;

		this.nadirPoint     = new ArrayList<Double>(this.getProblem().getNumberOfObjectives());
		this.initializeNadirPoint(this.getProblem().getNumberOfObjectives());
		this.utopiaPoint = new ArrayList<Double>(this.getProblem().getNumberOfObjectives());
		this.initializeUtopiaPoint(this.getProblem().getNumberOfObjectives());
	}

	@Override
	protected void initProgress() {
		this.iterations = 1;
	}

	@Override
	protected void updateProgress() {
		this.iterations+=1;
	}

	@Override
	protected boolean isStoppingConditionReached() {
		return this.iterations >= this.maxIterations;
	}

	@Override
	protected List<S> createInitialPopulation() {
		List<S> population = new ArrayList<>(this.getPopulationSize());
		for (int i = 0; i < this.getPopulationSize(); i++) {
			S newIndividual = problem.createSolution();
			population.add(newIndividual);
		}
		return population;
	}

	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		population = evaluator.evaluate(population, problem);

		return population;
	}

	@Override
	protected List<S> selection(List<S> population) {
		List<S> matingPopulation = new ArrayList<>(population.size());
		for (int i = 0; i < this.getPopulationSize(); i++) {
			S solution = selectionOperator.execute(population);
			matingPopulation.add(solution);
		}

		return matingPopulation;
	}

	@Override
	protected List<S> reproduction(List<S> population) {
		List<S> offspringPopulation = new ArrayList<>(this.getPopulationSize());
		for (int i = 0; i < this.getPopulationSize(); i += 2) {
			List<S> parents = new ArrayList<>(2);
			int parent1Index = JMetalRandom.getInstance().nextInt(0, this.getPopulationSize()-1);
			int parent2Index = JMetalRandom.getInstance().nextInt(0, this.getPopulationSize()-1);
			while (parent1Index==parent2Index)
				parent2Index = JMetalRandom.getInstance().nextInt(0, this.getPopulationSize()-1);
			parents.add(population.get(parent1Index));
			parents.add(population.get(parent2Index));

			List<S> offspring = crossoverOperator.execute(parents);

			mutationOperator.execute(offspring.get(0));
			mutationOperator.execute(offspring.get(1));

			offspringPopulation.add(offspring.get(0));
			offspringPopulation.add(offspring.get(1));
		}
		return offspringPopulation;
	}

	@Override
	public List<S> getResult() {
		this.setPopulation(evaluator.evaluate(this.getPopulation(), problem));

		return this.getPopulation();
	}

	@Override
	public void run() {
		List<S> offspringPopulation;
		List<S> matingPopulation;

		this.setPopulation(createInitialPopulation());
		this.evaluatePopulation(this.getPopulation());
		initProgress();
		//specific GA needed computations
		this.specificMOEAComputations();
		while (!isStoppingConditionReached()) {
			matingPopulation = selection(this.getPopulation());
			offspringPopulation = reproduction(matingPopulation);
			offspringPopulation = evaluatePopulation(offspringPopulation);
			// specific GA needed computations
			this.specificMOEAComputations();
			this.setPopulation(replacement(this.getPopulation(), offspringPopulation));
			updateProgress();
			
		}
	}

	public abstract void specificMOEAComputations();

	public Problem<S> getProblem() {
		return this.problem;
	}

	public List<Double> getUtopiaPoint() {
		return this.utopiaPoint;
	}

	public List<Double> getNadirPoint() {
		return this.nadirPoint;
	}

	private void initializeUtopiaPoint(int size) {
		for (int i = 0; i < size; i++)
			this.getUtopiaPoint().add(Double.POSITIVE_INFINITY);
	}

	private void initializeNadirPoint(int size) {
    for (int i = 0; i < size; i++)
			this.getNadirPoint().add(Double.NEGATIVE_INFINITY);
	}

	protected void updateUtopiaPoint(S s) {
		for (int i = 0; i < s.getNumberOfObjectives(); i++) 
			this.getUtopiaPoint().set(i, Math.min(this.getUtopiaPoint().get(i),s.getObjective(i)));			
	}

	private void updateNadirPoint(S s) {
		for (int i = 0; i < s.getNumberOfObjectives(); i++)
			this.getNadirPoint().set(i, Math.max(this.getNadirPoint().get(i),s.getObjective(i)));
	}

	public void updateUtopiaPoint(List<S> population) {
		for (S solution : population)
			this.updateUtopiaPoint(solution);
	}

	public void updateNadirPoint(List<S> population) {
		for (S solution : population)
			this.updateNadirPoint(solution);
	}

	protected abstract int getPopulationSize();

	protected boolean populationIsNotFull(List<S> population) {
		return population.size() < getPopulationSize();
	}

	protected void setUtopiaPointValue(Double value, int index) {
		if ((index < 0) || (index >= this.utopiaPoint.size())) {
			throw new IndexOutOfBoundsException();
		}

		this.utopiaPoint.set(index, value);
	}
}
