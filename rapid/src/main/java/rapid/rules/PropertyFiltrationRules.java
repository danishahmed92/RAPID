package rapid.rules;

import java.util.HashSet;
import java.util.Set;

public class PropertyFiltrationRules {
    public static final Set<String> NON_SYMMETRIC_PROPERTIES = new HashSet<String>() {{
        add("child");
        add("district");
        add("doctoralAdvisor");
        add("doctoralStudent");
        add("locatedInArea");
        add("parent");
        add("trainer");
        add("affiliation");
    }};
}
