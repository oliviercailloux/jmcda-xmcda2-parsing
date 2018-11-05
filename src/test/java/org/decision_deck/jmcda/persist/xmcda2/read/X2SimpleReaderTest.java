package org.decision_deck.jmcda.persist.xmcda2.read;

import static org.junit.Assert.assertEquals;

import org.decision_deck.jmcda.structure.Alternative;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.matrix.Evaluations;
import org.decision_deck.jmcda.structure.matrix.EvaluationsUtils;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.X2SimpleReader;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;
import org.decisiondeck.jmcda.structure.sorting.problem.preferences.ISortingPreferences;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class X2SimpleReaderTest {
    @Test
    public void testOne() throws Exception {
	final X2SimpleReader reader = new X2SimpleReader(
		new XMCDAReadUtils().getSample("Small examples/One alternative.xml"));
	final ISortingData read = reader.readSortingData();
	assertEquals(ImmutableSet.of(getA1()), read.getAlternatives());
	final Evaluations expected = EvaluationsUtils.newEvaluationMatrix();
	expected.put(getA1(), getG1(), 1);
	assertEquals(expected, read.getAlternativesEvaluations());
    }

    private Criterion getG1() {
	return new Criterion("g1");
    }

    private Alternative getP1() {
	return new Alternative("p1");
    }

    @Test
    public void testOneWithProfileFromEvaluations() throws Exception {
        final X2SimpleReader reader = new X2SimpleReader(
        	new XMCDAReadUtils().getSample("Small examples/One alternative and one profile - From evaluations.xml"));
	final ISortingPreferences read = reader.readSortingPreferences();
        assertEquals(ImmutableSet.of(getA1()), read.getAlternatives());
	final Evaluations expectedA = EvaluationsUtils.newEvaluationMatrix();
        expectedA.put(getA1(), getG1(), 1);
        assertEquals(expectedA, read.getAlternativesEvaluations());

	assertEquals(ImmutableSet.of(getP1()), read.getProfiles());
	final Evaluations expectedP = EvaluationsUtils.newEvaluationMatrix();
	expectedP.put(getP1(), getG1(), 0);
	assertEquals(expectedP, read.getProfilesEvaluations());
    }

    @Test
    public void testOneWithConcept() throws Exception {
        final X2SimpleReader reader = new X2SimpleReader(
        	new XMCDAReadUtils().getSample("Small examples/One alternative - Concept.xml"));
        final ISortingPreferences read = reader.readSortingPreferences();
        assertEquals(ImmutableSet.of(getA1()), read.getAlternatives());
	final Evaluations expected = EvaluationsUtils.newEvaluationMatrix();
        expected.put(getA1(), getG1(), 1);
        assertEquals(expected, read.getAlternativesEvaluations());
    
        assertEquals(ImmutableSet.of(), read.getProfiles());
	assertEquals(EvaluationsUtils.newEvaluationMatrix(), read.getProfilesEvaluations());
    }

    @Test(expected=InvalidInputException.class)
    public void testOneWithConceptsInvalid() throws Exception {
        final X2SimpleReader reader = new X2SimpleReader(
        	new XMCDAReadUtils().getSample("Small examples/One alternative - Concepts, invalid.xml"));
        final ISortingPreferences read = reader.readSortingPreferences();
        assertEquals(ImmutableSet.of(getA1()), read.getAlternatives());
	final Evaluations expected = EvaluationsUtils.newEvaluationMatrix();
        expected.put(getA1(), getG1(), 1);
        assertEquals(expected, read.getAlternativesEvaluations());
    
        assertEquals(ImmutableSet.of(), read.getProfiles());
	assertEquals(EvaluationsUtils.newEvaluationMatrix(), read.getProfilesEvaluations());
    }

    @Test
    public void testOneWithConcepts() throws Exception {
        final X2SimpleReader reader = new X2SimpleReader(
        	new XMCDAReadUtils().getSample("Small examples/One alternative - Concepts.xml"));
        final ISortingPreferences read = reader.readSortingPreferences();
        assertEquals(ImmutableSet.of(getA1()), read.getAlternatives());
	final Evaluations expected = EvaluationsUtils.newEvaluationMatrix();
        expected.put(getA1(), getG1(), 1);
        assertEquals(expected, read.getAlternativesEvaluations());
    
        assertEquals(ImmutableSet.of(), read.getProfiles());
	assertEquals(EvaluationsUtils.newEvaluationMatrix(), read.getProfilesEvaluations());
    }

    @Test
    public void testOneWithPreferences() throws Exception {
        final X2SimpleReader reader = new X2SimpleReader(
        	new XMCDAReadUtils().getSample("Small examples/One alternative.xml"));
        final ISortingPreferences read = reader.readSortingPreferences();
        assertEquals(ImmutableSet.of(getA1()), read.getAlternatives());
	final Evaluations expected = EvaluationsUtils.newEvaluationMatrix();
        expected.put(getA1(), getG1(), 1);
        assertEquals(expected, read.getAlternativesEvaluations());
    
        assertEquals(ImmutableSet.of(), read.getProfiles());
	assertEquals(EvaluationsUtils.newEvaluationMatrix(), read.getProfilesEvaluations());
    }

    private Alternative getA1() {
        return new Alternative("a1");
    }

    @Test
    public void testOneWithProfile() throws Exception {
        final X2SimpleReader reader = new X2SimpleReader(
        	new XMCDAReadUtils().getSample("Small examples/One alternative and one profile.xml"));
        final ISortingPreferences read = reader.readSortingPreferences();
        assertEquals(ImmutableSet.of(getA1()), read.getAlternatives());
	final Evaluations expectedA = EvaluationsUtils.newEvaluationMatrix();
        expectedA.put(getA1(), getG1(), 1);
        assertEquals(expectedA, read.getAlternativesEvaluations());
    
        assertEquals(ImmutableSet.of(getP1()), read.getProfiles());
	final Evaluations expectedP = EvaluationsUtils.newEvaluationMatrix();
        expectedP.put(getP1(), getG1(), 0);
        assertEquals(expectedP, read.getProfilesEvaluations());
    }

    @Test
    public void testOneWithProfileSeparate() throws Exception {
        final X2SimpleReader reader = new X2SimpleReader(
        	new XMCDAReadUtils().getSample("Small examples/One alternative and one profile - Separate.xml"));
        final ISortingPreferences read = reader.readSortingPreferences();
        assertEquals(ImmutableSet.of(getA1()), read.getAlternatives());
	final Evaluations expectedA = EvaluationsUtils.newEvaluationMatrix();
        expectedA.put(getA1(), getG1(), 1);
        assertEquals(expectedA, read.getAlternativesEvaluations());
    
        assertEquals(ImmutableSet.of(getP1()), read.getProfiles());
	final Evaluations expectedP = EvaluationsUtils.newEvaluationMatrix();
        expectedP.put(getP1(), getG1(), 0);
        assertEquals(expectedP, read.getProfilesEvaluations());
    }
}
