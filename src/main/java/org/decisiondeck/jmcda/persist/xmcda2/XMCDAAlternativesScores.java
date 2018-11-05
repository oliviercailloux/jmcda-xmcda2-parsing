package org.decisiondeck.jmcda.persist.xmcda2;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.scores.AlternativesScores;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativeValue;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XAlternativesValues;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XValue;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;

import com.google.common.collect.Sets;

/**
 * Methods for reading and writing alternative scores from and to XMCDA fragments.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XMCDAAlternativesScores extends XMCDAHelperWithVarious {

    private final Set<Alternative> m_alternatives = Sets.newLinkedHashSet();

    /**
     * <p>
     * Retrieves a mapping between the alternatives and the scores found in the given fragment.
     * </p>
     * <p>
     * In case of unexpected data, an exception is thrown if this object follows the {@link ErrorManagement#THROW}
     * strategy, otherwise, non conforming informations will be skipped.
     * </p>
     * 
     * @param xAlternativesValuesList
     *            not <code>null</code>.
     * @return not <code>null</code>.
     * @throws InvalidInputException
     *             iff unexpected content has been read and this object follows the {@link ErrorManagement#THROW}
     *             strategy.
     */
    public AlternativesScores readScoredAlternatives(Collection<XAlternativesValues> xAlternativesValuesList)
	    throws InvalidInputException {
	checkNotNull(xAlternativesValuesList);
	final AlternativesScores scores = new AlternativesScores();
	for (final XAlternativesValues xAlternativesValues : xAlternativesValuesList) {
	    final List<XAlternativeValue> xAlternativeValueList = xAlternativesValues.getAlternativeValueList();
	    for (final XAlternativeValue xAlternativeValue : xAlternativeValueList) {
		final String id = xAlternativeValue.getAlternativeID();
		if (id == null || id.isEmpty()) {
		    continue;
		}
		final Alternative alternative = new Alternative(id);
		if (scores.get(alternative) != null) {
		    error("Duplicate " + alternative + " score.");
		    continue;
		}
		final List<XValue> xValueList = xAlternativeValue.getValueList();
		final XValue xValue = getUnique(xValueList, xAlternativeValue.toString());
		if (xValue == null) {
		    continue;
		}
		final Double score = readDouble(xValue);
		if (score == null) {
		    continue;
		}
		scores.put(alternative, score);
	    }
	}
	return scores;
    }

    /**
     * Retrieves the XMCDA equivalent of the given scores.
     * 
     * @param scoredAlternatives
     *            not <code>null</code>.
     * @return not <code>null</code>.
     */
    public XAlternativesValues writeScoredAlternatives(AlternativesScores scoredAlternatives) {
	final XAlternativesValues xAlternativesValues = XMCDA.Factory.newInstance().addNewAlternativesValues();
	for (Alternative alternative : scoredAlternatives.keySet()) {
	    final double score = scoredAlternatives.get(alternative).doubleValue();
	    final XAlternativeValue xAlternativeValue = xAlternativesValues.addNewAlternativeValue();
	    xAlternativeValue.setAlternativeID(alternative.getId());
	    final XValue xValue = xAlternativeValue.addNewValue();
	    xValue.setReal((float) score);
	}
	return xAlternativesValues;
    }

    /**
     * Retrieves a writeable view of the set of alternatives stored in this object.
     * 
     * @return not <code>null</code>.
     */
    public Set<Alternative> getAlternatives() {
	return m_alternatives;
    }

    /**
     * Sets the alternatives stored in this object as the given ones. No reference is held to the given set.
     * 
     * @param alternatives
     *            not <code>null</code>, may be empty.
     */
    public void setAlternatives(Set<Alternative> alternatives) {
	checkNotNull(alternatives);
	m_alternatives.clear();
	m_alternatives.addAll(alternatives);
    }

    /**
     * Creates a new object which will use the default error management strategy {@link ErrorManagement#THROW}.
     */
    public XMCDAAlternativesScores() {
	super();
    }

    /**
     * Creates a new object delegating error management to the given error manager in case of unexpected data read.
     * 
     * @param errorsManager
     *            not <code>null</code>.
     */
    public XMCDAAlternativesScores(XMCDAErrorsManager errorsManager) {
	super(errorsManager);
    }
}
