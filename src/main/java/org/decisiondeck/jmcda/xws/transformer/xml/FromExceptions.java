package org.decisiondeck.jmcda.xws.transformer.xml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.XMCDAVarious;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMCDADoc.XMCDA;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMessage;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XMethodMessages;

import com.google.common.base.Function;

public class FromExceptions implements Function<List<InvalidInputException>, XMethodMessages> {
	private static class ExcToDetailedStr implements Function<InvalidInputException, String> {
		public ExcToDetailedStr() {
			/** Public default constructor. */
		}

		@Override
		public String apply(InvalidInputException exc) {
			final StringWriter out = new StringWriter();
			try (PrintWriter s = new PrintWriter(out)) {
				exc.printStackTrace(s);
			}
			return out.toString();
		}
	}

	private static class ExcToShortStr implements Function<InvalidInputException, String> {
		public ExcToShortStr() {
			/** Public default constructor. */
		}

		@Override
		public String apply(InvalidInputException exc) {
			final StringBuffer out = new StringBuffer();
			final String message = exc.getMessage();
			out.append(exc.getClass().getSimpleName());
			out.append(": ");
			out.append(message);
			out.append('\n');
			for (Throwable current = exc.getCause(); current != null; current = current.getCause()) {
				out.append("Caused by: ");
				out.append(current.getMessage());
				out.append('\n');
			}
			return out.toString();
		}
	}

	@Override
	public XMethodMessages apply(List<InvalidInputException> input) {
		if (input.isEmpty()) {
			final XMessage xMsg = new XMCDAVarious().writeLogMessage("Everything is ok.");
			final XMethodMessages x = XMCDA.Factory.newInstance().addNewMethodMessages();
			x.addNewLogMessage().set(xMsg);
			return x;
		}
		final ExcToDetailedStr toDetailed = new ExcToDetailedStr();
		final ExcToShortStr toShort = new ExcToShortStr();
		final XMCDAVarious writer = new XMCDAVarious();
		final XMethodMessages xMethodMessages = XMCDA.Factory.newInstance().addNewMethodMessages();
		for (InvalidInputException err : input) {
			final String detailed = toDetailed.apply(err);
			final String asShort = toShort.apply(err);
			final XMessage xShort = writer.writeErrorMessage(asShort);
			xMethodMessages.addNewErrorMessage().set(xShort);
			final XMessage xDetailed = writer.writeLogMessage(detailed);
			xMethodMessages.addNewLogMessage().set(xDetailed);
		}
		// writer.writeErrorMessages(Collections2.transform(input, new
		// ExcToDetailedStr()))
		return xMethodMessages;
	}
}