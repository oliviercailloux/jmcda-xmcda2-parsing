package org.decisiondeck.jmcda.persist.xmcda2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlCursor.TokenType;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.decision_deck.jmcda.structure.Criterion;
import org.decision_deck.jmcda.structure.interval.Interval;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriteria;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriterion;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.sample_problems.SixRealCars;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class XMCDACriteriaTest {
	private static final Logger s_logger = LoggerFactory.getLogger(XMCDACriteriaTest.class);

	@Test
	public void testCars() throws Exception {
		final InputStream input = getClass().getResourceAsStream("SixRealCars with criteriaSet.xml");
		final XMCDADoc xmcdaDoc = XMCDADoc.Factory.parse(input);
		final List<XCriteria> xCriteriaList = xmcdaDoc.getXMCDA().getCriteriaList();
		final XCriteria xCriteria = Iterables.getOnlyElement(xCriteriaList);

		final XMCDACriteria reader = new XMCDACriteria();
		final Set<Criterion> criteria = reader.read(xCriteria);

		final SixRealCars data = SixRealCars.getInstance();
		final Set<Criterion> expectedCriteria = data.getCriteria();
		assertEquals(expectedCriteria, criteria);

		final LinkedHashSet<Criterion> expectedOrder = Sets.newLinkedHashSet();
		expectedOrder.add(new Criterion("c01"));
		expectedOrder.add(new Criterion("c03"));
		expectedOrder.add(new Criterion("c04"));
		expectedOrder.add(new Criterion("c02"));
		expectedOrder.add(new Criterion("c05"));
		Iterables.elementsEqual(expectedOrder, criteria);

		final Map<Criterion, Interval> expectedScales = data.getScales();
		assertEquals(expectedScales, reader.getScales());
	}

	@Test
	public void testOnlyCriteria() throws Exception {
		final ByteSource supplier = Resources.asByteSource(getClass().getResource("Criteria.xml"));
		final XMCDA xmcda = new XMCDAReadUtils().getXMCDA(supplier);
		final List<XCriteria> xCriteriaList = xmcda.getCriteriaList();
		final XCriteria xCriteria = Iterables.getOnlyElement(xCriteriaList);

		final XMCDACriteria reader = new XMCDACriteria();
		final Set<Criterion> criteria = reader.read(xCriteria);

		final LinkedHashSet<Criterion> expectedOrder = Sets.newLinkedHashSet();
		expectedOrder.add(new Criterion("g2"));
		expectedOrder.add(new Criterion("g1"));
		expectedOrder.add(new Criterion("g3"));
		Iterables.elementsEqual(expectedOrder, criteria);

		assertEquals(0, reader.getScales().size());
	}

	@Test(expected = InvalidInputException.class)
	public void testReadDuplicateId() throws Exception {
		final InputStream input = getClass().getResourceAsStream("Criteria - Duplicate.xml");
		final XMCDADoc xmcdaDoc = XMCDADoc.Factory.parse(input);
		final List<XCriteria> xCriteriaList = xmcdaDoc.getXMCDA().getCriteriaList();
		final XCriteria xCriteria = Iterables.getOnlyElement(xCriteriaList);
		new XMCDACriteria().read(xCriteria);
	}

	@Test
	public void testWriteEmpty() throws Exception {
		final XCriteria xC = XMCDACriteria.write(Collections.<Criterion> emptySet(), null, null);
		getAsXmlStringWithOuterText(xC);
		getAsFullString(xC);
		readContent(xC);
		final XmlCursor cursor = xC.newCursor();
		/** This is in case the returned xml does not start with "criteria" */
		// assertTrue(cursor.isStartdoc());
		// assertTrue(cursor.toNextToken().isEnddoc());
		// assertFalse(cursor.hasNextToken());

		assertEquals(new QName("criteria"), cursor.getName());
		assertTrue(cursor.toNextToken().isEnd());
		assertTrue(cursor.toNextToken().isEnddoc());
		assertFalse(cursor.hasNextToken());
	}

	@Test
	public void testWriteOne() throws Exception {
		final Criterion g1 = new Criterion("g1");
		final XCriteria xCs = XMCDACriteria.write(Collections.singleton(g1), null, null);
		final String criteriaXmlInner = xCs.toString();
		assertEquals(criteriaXmlInner, "<criterion id=\"g1\"/>");
		final String criteriaXmlWithOuter = getAsXmlStringWithOuterText(xCs);
		assertEquals(criteriaXmlWithOuter, "<criteria><criterion id=\"g1\"/></criteria>");
	}

	@Test
	public void testWriteTwo() throws Exception {
		final Criterion g1 = new Criterion("g1");
		final Criterion g2 = new Criterion("g2");
		final Set<Criterion> criteria = Sets.newLinkedHashSet();
		criteria.add(g1);
		criteria.add(g2);
		final XCriteria xCs = XMCDACriteria.write(criteria, null, null);

		assertEquals(getAsXmlStringWithOuterText(xCs),
				"<criteria><criterion id=\"g1\"/><criterion id=\"g2\"/></criteria>");
		final XCriterion xC = xCs.getCriterionArray(0);
		assertEquals(getAsXmlStringWithOuterText(xC), "<criterion id=\"g1\"/>");
	}

	String getAsFullString(XmlTokenSource x) throws IOException {
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();
		x.save(stream, new XmlOptions().setSaveOuter());
		final String str = stream.toString(Charsets.UTF_8.name());
		// final String str = x.xmlText(new XmlOptions().setSaveOuter());
		s_logger.info("As full string: {}.", str);
		return str;
	}

	String getAsXmlStringWithOuterText(XmlTokenSource xmlSource) {
		final String xmlText = xmlSource.xmlText(new XmlOptions().setSaveOuter());
		s_logger.info("Outer: {}.", xmlText);
		return xmlText;
	}

	void readContent(XCriteria xC) {
		final XmlCursor cursor = xC.newCursor();
		s_logger.info("Name: {}.", cursor.getName());
		while (cursor.hasNextToken()) {
			final TokenType type = cursor.toNextToken();
			s_logger.info("Type: {}, name: {}.", type, cursor.getName());
		}
	}
}
