package is.generador;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Totals for explicit declarations, including declarations in nested and local types. */
public final class ProjectSummary {
    private final Map<String, Integer> totals = new LinkedHashMap<>();
    private final Set<String> packages = new HashSet<>();

    ProjectSummary() {
        for (String name : new String[]{"Java files", "Packages", "Classes", "Abstract classes",
                "Anonymous classes", "Interfaces", "Enums", "Records", "Annotation types",
                "Methods", "Fields", "Constructors", "Getters", "Setters",
                "Enum constants", "Record components", "Annotation members"}) {
            totals.put(name, 0);
        }
    }

    void add(String name, int amount) {
        totals.merge(name, amount, Integer::sum);
    }

    void addPackage(String name) {
        packages.add(name);
        totals.put("Packages", packages.size());
    }

    public Map<String, Integer> getTotals() {
        return Collections.unmodifiableMap(totals);
    }

    public int getJavaFiles() { return totals.get("Java files"); }
    public int getClasses() { return totals.get("Classes"); }
    public int getMethods() { return totals.get("Methods"); }
    public int getFields() { return totals.get("Fields"); }
    public int getConstructors() { return totals.get("Constructors"); }
    public int getGetters() { return totals.get("Getters"); }
    public int getSetters() { return totals.get("Setters"); }
    public int getInterfaces() { return totals.get("Interfaces"); }
    public int getEnums() { return totals.get("Enums"); }
    public int getRecords() { return totals.get("Records"); }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        totals.forEach((name, amount) -> text.append(String.format("%-26s : %d%n", name, amount)));
        return text.toString();
    }
}
