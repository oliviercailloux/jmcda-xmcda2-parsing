package org.decisiondeck.jmcda.persist.xmcda2;

import org.decision_deck.jmcda.structure.interval.Interval;
import org.decision_deck.jmcda.structure.interval.Intervals;
import org.decision_deck.jmcda.structure.interval.PreferenceDirection;
import org.decisiondeck.jmcda.exc.InvalidInputException;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XCriterion;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XNumericValue;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XPreferenceDirection;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XQuantitative;
import org.decisiondeck.jmcda.persist.xmcda2.generated.XScale;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAErrorsManager.ErrorManagement;
import org.decisiondeck.jmcda.persist.xmcda2.utils.XMCDAHelperWithVarious;

import com.google.common.base.Preconditions;

/**
 * Methods for reading and writing continuous intervals informations (called
 * scales in XMCDA terms) from and to XMCDA fragments.
 *
 * @author Olivier Cailloux
 *
 */
public class XMCDAOrderedIntervals extends XMCDAHelperWithVarious {

	/**
	 * Creates a new object which will use the default error management strategy
	 * {@link ErrorManagement#THROW}.
	 */
	public XMCDAOrderedIntervals() {
		super();
	}

	/**
	 * Creates a new object delegating error management to the given error
	 * manager in case of unexpected data read.
	 *
	 * @param errorsManager
	 *            not <code>null</code>.
	 */
	public XMCDAOrderedIntervals(XMCDAErrorsManager errorsManager) {
		super(errorsManager);
	}

	/**
	 * Retrieves the direction equivalent to the given XMCDA direction.
	 *
	 * @param xDirection
	 *            not <code>null</code>.
	 * @return not <code>null</code>.
	 */
	public PreferenceDirection read(XPreferenceDirection.Enum xDirection) {
		Preconditions.checkNotNull(xDirection);

		switch (xDirection.intValue()) {
		case XPreferenceDirection.INT_MIN:
			return PreferenceDirection.MINIMIZE;
		case XPreferenceDirection.INT_MAX:
			return PreferenceDirection.MAXIMIZE;
		default:
			throw new IllegalStateException("Unknown xml direction: " + xDirection + ".");
		}
	}

	/**
	 * Retrieves the given scale as a proper interval.
	 *
	 * @param xScale
	 *            not <code>null</code>.
	 * @return a continuous scale, or <code>null</code> iff unexpected content
	 *         has been read and this object does not follow the
	 *         {@link ErrorManagement#THROW} strategy.
	 * @throws InvalidInputException
	 *             iff unexpected content has been read and this object follows
	 *             the {@link ErrorManagement#THROW} strategy.
	 */
	public Interval read(XScale xScale) throws InvalidInputException {
		final Interval scale;
		if (!xScale.isSetQuantitative()) {
			error("Expected quantitative scale instead of " + xScale + ".");
			return null;
		}
		final XQuantitative quantitative = xScale.getQuantitative();

		final XPreferenceDirection.Enum xDirection = quantitative.getPreferenceDirection();
		final PreferenceDirection direction;
		if (xDirection == null) {
			direction = null;
		} else {
			direction = read(xDirection);
		}

		final XNumericValue xmlMin = quantitative.getMinimum();
		final XNumericValue xmlMax = quantitative.getMaximum();
		final double min;
		final double max;
		if (xmlMin == null) {
			min = Double.NEGATIVE_INFINITY;
		} else {
			final Double minValue = readDouble(xmlMin);
			min = minValue == null ? Double.NEGATIVE_INFINITY : minValue.doubleValue();
		}
		if (xmlMax == null) {
			max = Double.POSITIVE_INFINITY;
		} else {
			final Double maxValue = readDouble(xmlMax);
			max = maxValue == null ? Double.POSITIVE_INFINITY : maxValue.doubleValue();
		}

		scale = Intervals.newUnrestrictedInterval(direction, min, max);
		return scale;
	}

	/**
	 * <p>
	 * Retrieves the given interval as an XMCDA scale.
	 * </p>
	 * <p>
	 * This method does not accept discrete intervals.
	 * </p>
	 *
	 * @param interval
	 *            not <code>null</code>, with no step size.
	 * @return <code>null</code> iff the given interval contains no information:
	 *         no preference direction and only infinite bounds.
	 */
	public XScale write(Interval interval) {
		Preconditions.checkNotNull(interval);
		Preconditions.checkArgument(interval.getStepSize() == null);

		final XScale xScale = XCriterion.Factory.newInstance().addNewScale();
		final XQuantitative xQuantScale = xScale.addNewQuantitative();

		final PreferenceDirection preferenceDirection = interval.getPreferenceDirection();
		final double minimum = interval.getMinimum();
		final double maximum = interval.getMaximum();
		if (preferenceDirection == null && Double.isInfinite(minimum) && Double.isInfinite(maximum)) {
			return null;
		}

		if (preferenceDirection != null) {
			xQuantScale.setPreferenceDirection(write(preferenceDirection));
		}

		if (!Double.isInfinite(minimum)) {
			final XNumericValue minValue = XNumericValue.Factory.newInstance();
			minValue.setReal((float) minimum);
			xQuantScale.setMinimum(minValue);
		}
		if (!Double.isInfinite(maximum)) {
			final XNumericValue maxValue = XNumericValue.Factory.newInstance();
			maxValue.setReal((float) maximum);
			xQuantScale.setMaximum(maxValue);
		}

		return xScale;
	}

	/**
	 * Retrieves the given preference direction as an XMCDA preference
	 * direction.
	 *
	 * @param preferenceDirection
	 *            not <code>null</code>, not unknown.
	 * @return not <code>null</code>.
	 */
	public XPreferenceDirection.Enum write(PreferenceDirection preferenceDirection) {
		Preconditions.checkNotNull(preferenceDirection);
		switch (preferenceDirection) {
		case MAXIMIZE:
			return XPreferenceDirection.MAX;
		case MINIMIZE:
			return XPreferenceDirection.MIN;
		default:
			throw new IllegalStateException();
		}
	}
}
