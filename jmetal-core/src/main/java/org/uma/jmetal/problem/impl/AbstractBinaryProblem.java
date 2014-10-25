package org.uma.jmetal.problem.impl;

import org.uma.jmetal.solution.BinarySolution;
import org.uma.jmetal.problem.BinaryProblem;

public abstract class AbstractBinaryProblem extends AbstractGenericProblem<BinarySolution>
  implements BinaryProblem {

  protected int [] bitsPerVariable ;

  public int getNumberOfBits(int index) {
    return bitsPerVariable[index] ;
  }

}