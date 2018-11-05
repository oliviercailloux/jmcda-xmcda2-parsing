package org.decisiondeck.jmcda.xws.transformer.xml;

import java.util.Collection;
import java.util.Map;

import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAssignments;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesAffectations;
import org.decisiondeck.jmcda.structure.sorting.assignment.IAssignmentsToMultiple;

public class ToAllAssignments implements
	FunctionWithInputCheck<Collection<XAlternativesAffectations>, Map<DecisionMaker, IAssignmentsToMultiple>> {
    @Override
    public Map<DecisionMaker, IAssignmentsToMultiple> apply(Collection<XAlternativesAffectations> input)
	    throws InvalidInputException {
	return input == null ? null : new XMCDAAssignments().readAll(input);
    }
}