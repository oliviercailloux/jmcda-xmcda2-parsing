package org.decisiondeck.jmcda.persist.xmcda2;

public enum X2Concept {
	FICTIVE, REAL, UNMARKED;

	static public X2Concept asConcept(String stringForm) {
		if ("real".equalsIgnoreCase(stringForm)) {
			return REAL;
		} else if ("fictive".equalsIgnoreCase(stringForm)) {
			return FICTIVE;
		} else if (stringForm == null) {
			return UNMARKED;
		} else {
			return null;
		}
	}

	public boolean matches(String stringForm) {
		switch (this) {
		case REAL:
			return "real".equalsIgnoreCase(stringForm);
		case FICTIVE:
			return "fictive".equalsIgnoreCase(stringForm);
		case UNMARKED:
			return stringForm == null;
		default:
			throw new IllegalStateException();
		}
	}
}