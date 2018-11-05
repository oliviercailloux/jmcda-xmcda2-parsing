package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decisiondeck.jmcda.persist.xmcda2.XMCDAAssignments;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesAffectations;
import org.decisiondeck.jmcda.structure.sorting.assignment.IOrderedAssignmentsToMultipleRead;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

public class FromAffectations implements Function<IOrderedAssignmentsToMultipleRead, XAlternativesAffectations> {
    @Override
    public XAlternativesAffectations apply(IOrderedAssignmentsToMultipleRead input) {
	Preconditions.checkState(input.getAlternatives().size() >= 1, "Can't write empty assignments.");
	return new XMCDAAssignments().write(input);
    }
}