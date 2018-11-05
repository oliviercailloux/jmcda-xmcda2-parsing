package org.decisiondeck.jmcda.xws.transformer.xml;

import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDACategories;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesProfiles;

public class ToCategoriesAndProfiles implements FunctionWithInputCheck<XCategoriesProfiles, CatsAndProfs> {
    @Override
    public CatsAndProfs apply(XCategoriesProfiles input) throws InvalidInputException {
	return input == null ? null : new XMCDACategories().read(input);
    }

}
