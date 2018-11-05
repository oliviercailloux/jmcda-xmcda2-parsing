package transform;

import java.io.File;
import java.util.Map;

import org.decision_deck.jmcda.structure.DecisionMaker;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAEvaluations;
import org.decisiondeck.jmcda.persist.xmcda2.aggregates.X2SimpleReader;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPerformanceTable;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAReadUtils;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAWriteUtils;
import org.decisiondeck.jmcda.structure.sorting.assignment.credibilities.IAssignmentsWithCredibilities;
import org.decisiondeck.jmcda.structure.sorting.problem.data.ISortingData;

import com.google.common.io.Files;

public class TransformPollutantsData {

	private static final String PATH = "/home/olivier/Recherche/Décision de groupe/Étude substances/Séance 2/Données/data.xml";

	public static void main(String[] args) throws Exception {
		new TransformPollutantsData().proceed();
	}

	public void proceed() throws Exception {
		final X2SimpleReader reader = new X2SimpleReader(
				new XMCDAReadUtils().getXMCDA(Files.asByteSource(new File(PATH))));
		final ISortingData data = reader.readSortingData();
		final Map<DecisionMaker, IAssignmentsWithCredibilities> groupAssignments = reader
				.readGroupAssignmentsWithCredibilities();

		final XPerformanceTable writtenPerfs = new XMCDAEvaluations().write(data.getAlternativesEvaluations());
		XMCDAWriteUtils.write(writtenPerfs, Files.asByteSink(new File("out.xml")), true);
	}

}
