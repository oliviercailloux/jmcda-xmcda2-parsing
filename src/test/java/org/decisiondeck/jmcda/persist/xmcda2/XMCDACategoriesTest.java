package org.decisiondeck.jmcda.persist.xmcda2;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.List;

import org.decision_deck.jmcda.services.generator.DataGenerator;
import org.decision_deck.jmcda.structure.sorting.category.CatsAndProfs;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCategoriesProfiles;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.junit.Test;

import com.google.common.collect.Iterables;

public class XMCDACategoriesTest {
    @Test
    public void test3Cats() throws Exception {
	final InputStream input = getClass().getResourceAsStream("Category profiles - Three.xml");
	final XMCDADoc xmcdaDoc = XMCDADoc.Factory.parse(input);
	final List<XCategoriesProfiles> xCategoriesProfilesList = xmcdaDoc.getXMCDA().getCategoriesProfilesList();
	final XCategoriesProfiles xCategoriesProfiles = Iterables.getOnlyElement(xCategoriesProfilesList);

	final XMCDACategories reader = new XMCDACategories();
	final CatsAndProfs read = reader.read(xCategoriesProfiles);

	assertEquals(new DataGenerator().genCatsAndProfs(3), read);
    }

    @Test
    public void test2Cats() throws Exception {
	final InputStream input = getClass().getResourceAsStream("Category profiles - Two.xml");
	final XMCDADoc xmcdaDoc = XMCDADoc.Factory.parse(input);
	final List<XCategoriesProfiles> xCategoriesProfilesList = xmcdaDoc.getXMCDA().getCategoriesProfilesList();
	final XCategoriesProfiles xCategoriesProfiles = Iterables.getOnlyElement(xCategoriesProfilesList);

	final XMCDACategories reader = new XMCDACategories();
	final CatsAndProfs read = reader.read(xCategoriesProfiles);

	assertEquals(new DataGenerator().genCatsAndProfs(2), read);
    }

    @Test(expected = InvalidInputException.class)
    public void testNotConnex() throws Exception {
	final InputStream input = getClass().getResourceAsStream("Category profiles - Not connex.xml");
	final XMCDADoc xmcdaDoc = XMCDADoc.Factory.parse(input);
	final List<XCategoriesProfiles> xCategoriesProfilesList = xmcdaDoc.getXMCDA().getCategoriesProfilesList();
	final XCategoriesProfiles xCategoriesProfiles = Iterables.getOnlyElement(xCategoriesProfilesList);

	final XMCDACategories reader = new XMCDACategories();
	final CatsAndProfs read = reader.read(xCategoriesProfiles);

	assertEquals(new DataGenerator().genCatsAndProfs(2), read);
    }
}
